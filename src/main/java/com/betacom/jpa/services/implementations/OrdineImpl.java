package com.betacom.jpa.services.implementations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.betacom.jpa.dto.input.OrdineReq;
import com.betacom.jpa.dto.output.OrdineDTO;
import com.betacom.jpa.enums.StatoOrdine;
import com.betacom.jpa.enums.StatoPagamento;
import com.betacom.jpa.exceptions.ApiException;
import com.betacom.jpa.mapping.CouponMap;
import com.betacom.jpa.mapping.OrdineMap;
import com.betacom.jpa.models.Carrello;
import com.betacom.jpa.models.Coupon;
import com.betacom.jpa.models.DettaglioCarrello;
import com.betacom.jpa.models.DettaglioOrdine;
import com.betacom.jpa.models.Indirizzo;
import com.betacom.jpa.models.Ordine;
import com.betacom.jpa.models.VarianteProdotto;
import com.betacom.jpa.repositories.IDettaglioCarrelloRepository;
import com.betacom.jpa.repositories.IDettaglioOrdineRepository;
import com.betacom.jpa.repositories.IIndirizzoRepository;
import com.betacom.jpa.repositories.IOrdineRepository;
import com.betacom.jpa.repositories.IVarianteProdottoRepository;
import com.betacom.jpa.services.interfaces.ICarrelloServices;
import com.betacom.jpa.services.interfaces.ICouponServices;
import com.betacom.jpa.services.interfaces.IOrdineServices;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Proprietario: Valerio
@Slf4j
@RequiredArgsConstructor
@Service
public class OrdineImpl implements IOrdineServices {

	private final IOrdineRepository repOrd;
	private final IDettaglioOrdineRepository repDetOrd;
	private final IIndirizzoRepository repInd;
	private final IVarianteProdottoRepository repVar;
	private final IDettaglioCarrelloRepository repDetCar;
	private final ICarrelloServices carrelloS;
	private final ICouponServices couponS;
	private final EntityManager entityManager;

	@Transactional
	@Override
	public OrdineDTO checkout(Integer idUtente, OrdineReq req) throws Exception {
		log.debug("checkout {} / {}", idUtente, req);

		Carrello car = carrelloS.getOrCreateForUtente(idUtente);
		if (car.getRighe() == null || car.getRighe().isEmpty())
			throw new ApiException("carrello.empty");

		Indirizzo ind = repInd.findById(req.getIdIndirizzo())
				.orElseThrow(() -> new ApiException("indirizzo.ntfnd"));
		if (!ind.getUtente().getIdUtente().equals(idUtente))
			throw new ApiException("indirizzo.ntfnd");

		Coupon coupon = car.getCoupon() == null ? null : couponS.validateAndGet(car.getCoupon().getCodice(), idUtente);

		BigDecimal totaleProdotti = car.getRighe().stream()
				.map(r -> r.getVariante().getPrezzo().multiply(BigDecimal.valueOf(r.getQuantita())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal valoreSconto = coupon == null ? BigDecimal.ZERO : CouponMap.calcolaSconto(totaleProdotti, coupon);
		BigDecimal totalePagato = totaleProdotti.subtract(valoreSconto);

		Ordine ordine = new Ordine();
		ordine.setUtente(car.getUtente());
		ordine.setDataOrdine(LocalDateTime.now());
		ordine.setTotaleProdotti(totaleProdotti);
		ordine.setValoreSconto(valoreSconto);
		ordine.setTotalePagato(totalePagato);
		ordine.setCodiceCouponUsato(coupon == null ? null : coupon.getCodice());
		ordine.setStato(StatoOrdine.IN_ATTESA);
		ordine.setSpedizioneVia(ind.getVia());
		ordine.setSpedizioneCitta(ind.getCitta());
		ordine.setSpedizioneCap(ind.getCap());
		ordine.setSpedizioneProvincia(ind.getProvincia());
		ordine.setSpedizioneNazione(ind.getNazione());
		ordine.setMetodoPagamento(req.getMetodoPagamento());
		ordine.setStatoPagamento(StatoPagamento.DA_PAGARE);
		ordine.setRighe(new ArrayList<>());
		repOrd.save(ordine);

		// Un ordine stabile di acquisizione dei lock riduce il rischio di deadlock
		// quando due checkout contengono piu' varianti in comune.
		List<DettaglioCarrello> righeCarrello = car.getRighe().stream()
				.sorted(Comparator.comparing(r -> r.getVariante().getIdVariante()))
				.toList();
		for (DettaglioCarrello rigaCar : righeCarrello) {
			VarianteProdotto var = rigaCar.getVariante();

			int righeAggiornate = repVar.decrementaStock(
					var.getIdVariante(),
					rigaCar.getQuantita());
			if (righeAggiornate == 0)
				throw new ApiException("variante.stock.insufficient");

			// Le query UPDATE JPQL non aggiornano automaticamente l'entity gia'
			// caricata: refresh riallinea lo stock usato nel DTO di risposta.
			entityManager.refresh(var);

			DettaglioOrdine rigaOrd = new DettaglioOrdine();
			rigaOrd.setOrdine(ordine);
			rigaOrd.setVariante(var);
			rigaOrd.setQuantita(rigaCar.getQuantita());
			rigaOrd.setPrezzoUnitario(var.getPrezzo());
			repDetOrd.save(rigaOrd);
			ordine.getRighe().add(rigaOrd);
		}

		repDetCar.deleteAll(righeCarrello);
		car.getRighe().clear();
		car.setCoupon(null);

		return OrdineMap.buildOrdineDTO(ordine);
	}

	@Override
	public List<OrdineDTO> list(Integer callerId, boolean isAdmin, Integer idUtenteFiltro, String stato, String statoPagamento) throws Exception {
		log.debug("list caller:{} admin:{} utente:{} stato:{} statoPagamento:{}", callerId, isAdmin, idUtenteFiltro, stato, statoPagamento);

		Integer idUtente = isAdmin ? idUtenteFiltro : callerId;
		StatoOrdine s = stato == null ? null : StatoOrdine.valueOf(stato);
		StatoPagamento sp = statoPagamento == null ? null : StatoPagamento.valueOf(statoPagamento);

		return OrdineMap.buildOrdineDTOList(repOrd.searchByFilter(idUtente, s, sp));
	}

	@Override
	public OrdineDTO getById(Integer id, Integer callerId, boolean isAdmin) throws Exception {
		log.debug("getById {}", id);
		Ordine o = repOrd.findById(id)
				.orElseThrow(() -> new ApiException("ordine.ntfnd"));

		if (!isAdmin && !o.getUtente().getIdUtente().equals(callerId))
			throw new ApiException("ordine.forbidden");

		return OrdineMap.buildOrdineDTO(o);
	}

	@Transactional
	@Override
	public void updateStato(OrdineReq req) throws Exception {
		log.debug("updateStato {}", req);
		Ordine o = repOrd.findById(req.getId())
				.orElseThrow(() -> new ApiException("ordine.ntfnd"));
		Optional.ofNullable(req.getStato()).ifPresent(s -> o.setStato(StatoOrdine.valueOf(s)));
	}

	@Transactional
	@Override
	public void updateStatoPagamento(OrdineReq req) throws Exception {
		log.debug("updateStatoPagamento {}", req);
		Ordine o = repOrd.findById(req.getId())
				.orElseThrow(() -> new ApiException("ordine.ntfnd"));
		Optional.ofNullable(req.getStatoPagamento()).ifPresent(s -> o.setStatoPagamento(StatoPagamento.valueOf(s)));
	}

}
