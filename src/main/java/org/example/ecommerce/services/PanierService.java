package org.example.ecommerce.services;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.example.ecommerce.entities.Panier;
import org.example.ecommerce.entities.LignePanier;

import java.math.BigDecimal;
import java.util.Optional;

@Stateless
public class PanierService {

    @PersistenceContext(unitName = "ecommercePU")
    private EntityManager entityManager;

    @Transactional
    public void supprimerLigne(Long panierId, Long lignePanierId) {

        // 1. Charger le Panier dans la transaction
        Panier panier = entityManager.find(Panier.class, panierId);

        if (panier == null) {
            System.err.println("Erreur: Panier non trouvé avec ID: " + panierId);
            return;
        }

        // 2. Trouver et supprimer la LignePanier
        Optional<LignePanier> ligneToRemove = panier.getLignesPanier().stream()
                .filter(lp -> lp.getId().equals(lignePanierId))
                .findFirst();

        if (ligneToRemove.isPresent()) {
            // La LignePanier est retirée de la collection du Panier
            // Grâce à 'orphanRemoval=true' sur Panier.lignesPanier, Hibernate va DELETE la ligne
            panier.getLignesPanier().remove(ligneToRemove.get());

            // 3. Recalculer le montant total
            recalculerMontantTotal(panier);

            // Le merge n'est pas nécessaire si le Panier était déjà managé,
            // mais l'appel au service garantit que les changements sont propagés et committés.
            // entityManager.merge(panier); // Facultatif, le commit le fera.
        }
    }

    private void recalculerMontantTotal(Panier panier) {
        BigDecimal total = panier.getLignesPanier().stream()
                .map(LignePanier::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        panier.setMontantTotal(total);
    }
}