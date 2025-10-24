package org.example.ecommerce.services;


import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.example.ecommerce.entities.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Stateless
@Transactional
public class CommandeService {

    @PersistenceContext(unitName = "ecommercePU")
    private EntityManager entityManager;

    public void creerEtCommander(Client client, Panier panier) {

        // 1. Récupérer le client managé pour attacher la commande
        Client clientManaged = entityManager.find(Client.class, client.getId());
        if (clientManaged == null) {
            throw new RuntimeException("Client non trouvé lors de la commande.");
        }

        // 2. Créer l'objet Commande
        Commande commande = new Commande();
        commande.setClient(clientManaged);
        commande.setDateCommande(new Date());
        // Générer un numéro de commande unique (UUID ou autre séquence)
        commande.setNumeroCommande(UUID.randomUUID().toString().substring(0, 8));
        commande.setMontantTotal(panier.getMontantTotal()); // Récupérer le total calculé du panier
        commande.setStatut("EN_ATTENTE");
        // Note: L'adresse de livraison doit idéalement être fournie par le formulaire de checkout
        // Pour cet exemple, nous allons la définir à une valeur par défaut ou NULL si vous n'avez pas de champ.
        commande.setAdresseLivraison("Adresse non spécifiée");

        // 3. Créer et attacher les LignesCommande
        for (LignePanier lignePanier : panier.getLignesPanier()) {
            LigneCommande ligneCommande = new LigneCommande();

            // Copier les propriétés du Panier vers la Commande
            ligneCommande.setProduit(lignePanier.getProduit());
            ligneCommande.setQuantite(lignePanier.getQuantite());
            ligneCommande.setPrixUnitaire(lignePanier.getPrixUnitaire());
            ligneCommande.setSousTotal(lignePanier.getSousTotal());

            // Établir la relation bidirectionnelle
            ligneCommande.setCommande(commande);
            commande.getLignesCommande().add(ligneCommande);
        }

        // 4. Persister la Commande (et les LignesCommande via cascade=ALL)
        entityManager.persist(commande);

        // 5. Vider le Panier (c'est crucial après la commande)
        // Note: Vous devez avoir une relation bidirectionnelle Client-Panier-LignePanier correcte.

        // Vider la collection de lignes dans le panier
        panier.getLignesPanier().clear();
        panier.setMontantTotal(BigDecimal.ZERO);

        // Comme le panier est managé par le client et que les lignes ont orphanRemoval=true,
        // le clear() puis le commit/flush supprime toutes les lignes et met à jour le panier.
        entityManager.merge(panier); // Utilisez merge pour synchroniser les changements du panier

        // Le commit de la transaction à la fin de cette méthode garantit que
        // la commande est créée ET le panier est vidé, ou que rien n'est fait (atomicité).
    }

    public void supprimerCommande(Commande commande){
        try{
            // C'est l'approche la plus sûre pour éviter l'erreur "Entity must be managed to remove".
            Commande commandeManagée = entityManager.find(Commande.class, commande.getId());

            if(commande != null) {
                entityManager.remove(commandeManagée);
            }
            else{
                System.out.println("ERROR : la commande n'est pas trouvé");
            }


        }
        catch(Exception e){
            System.out.println("erreur commande: " + e.getMessage());
            // Il est préférable de jeter une RuntimeException pour déclencher le Rollback
            throw new RuntimeException("Échec de la suppression de la commande.", e);
        }
    }
}
