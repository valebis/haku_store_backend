package com.betacom.jpa.repositories;

import java.util.List;
import java.util.Optional;

// import di Spring Data JPA (repository generico, query custom) e dell'entity da gestire
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.betacom.jpa.models.VarianteProdotto;

// Proprietario: Mattia
@Repository    // dice a Spring che questa è una repository, gestita in automatico
public interface IVarianteProdottoRepository extends JpaRepository<VarianteProdotto, Integer> {
	// tutte le varianti di un certo prodotto
	List<VarianteProdotto> findByProdottoIdProdotto(Integer idProdotto);

	// true se esiste già una variante con stesso prodotto, gusto, formato e colore (evita di creare doppioni)
	boolean existsByProdottoIdProdottoAndGustoAndFormatoAndColore(Integer idProdotto, String gusto, String formato, String colore);

	Optional<VarianteProdotto> findByProdottoIdProdottoAndGustoAndFormatoAndColore(
		    Integer integer,
		    String gusto,
		    String formato,
		    String colore
		);
	// varianti tornate disponibili dopo essere state esaurite, dalla più recente, per la sezione "Nuovamente disponibile" della home
	@Query(name = "variante.selectNuovamenteDisponibili")
	List<VarianteProdotto> selectNuovamenteDisponibili();

	// Controlla e decrementa lo stock in un'unica operazione atomica sul database.
	@Modifying(flushAutomatically = true)
	@Query("""
			UPDATE VarianteProdotto v
			SET v.quantitaDisponibile = v.quantitaDisponibile - :quantita
			WHERE v.idVariante = :idVariante
			  AND v.quantitaDisponibile >= :quantita
			""")
	int decrementaStock(
			@Param("idVariante") Integer idVariante,
			@Param("quantita") Integer quantita);
}
