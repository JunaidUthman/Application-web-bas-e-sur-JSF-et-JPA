package org.example.ecommerce.beans;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.example.ecommerce.entities.Client;
import org.example.ecommerce.entities.LignePanier;
import org.example.ecommerce.entities.Panier;
import org.example.ecommerce.entities.Produit;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Named("accueilBean")
@Stateless
public class AccueilBean implements Serializable {

    @PersistenceContext(unitName = "ecommercePU")
    private EntityManager entityManager;

    private List<Produit> produits;
    private Client clientConnecte; // À gérer avec l'authentification

    @PostConstruct
    public void init() {
        chargerProduits();
    }

    /**
     * Récupère tous les produits depuis la base de données
     */
    public void chargerProduits() {
        try {
            produits = entityManager.createQuery(
                    "SELECT p FROM Produit p ",
                    Produit.class
            ).getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            // Gérer l'erreur (message à l'utilisateur, log, etc.)
        }
    }

    public String ajouterAuPanier(Produit produit) {

        FacesContext facesContext = FacesContext.getCurrentInstance();

        // --- 1. VÉRIFICATION DE L'AUTHENTIFICATION ---

        // Récupérer le client stocké dans la session sous la clé "loggedUser"
        Client loggedUser = (Client) facesContext.getExternalContext().getSessionMap().get("loggedUser");

        if (loggedUser == null) {
            // L'utilisateur n'est pas connecté. Redirection vers la page de login.
            System.out.println("Ajout au Panier ÉCHOUÉ: Utilisateur non authentifié. Redirection vers login.");
            return "/login?faces-redirect=true";
        }

        try {
            // Le client authentifié doit être rattaché au contexte de persistance (managed)
            // car l'objet 'loggedUser' vient de la session (détaché).
            Client clientManaged = entityManager.find(Client.class, loggedUser.getId());

            // Si pour une raison étrange l'utilisateur n'existe plus en DB, on redirige.
            if (clientManaged == null) {
                return "/public/login?faces-redirect=true";
            }

            // --- 2. RÉCUPÉRATION DU PANIER ---

            // Le panier doit exister car il est créé lors de l'inscription (voir corrections précédentes).
            Panier panier = clientManaged.getPanier();
            if (panier == null) {
                System.out.println("Erreur : la panier n'existe pas!!");
                return null;
            }

            // Le produit doit aussi être rattaché au contexte.
            Produit produitManaged = entityManager.find(Produit.class, produit.getId());
            if (produitManaged == null) {
                // Gérer le cas où le produit n'existe plus (facultatif mais bonne pratique)
                System.err.println("ERREUR: Produit non trouvé avec ID: " + produit.getId());
                return null;
            }

            // --- 3. GESTION DE LA LIGNE DE PANIER ---


                // Nouveau produit : Créer une nouvelle LignePanier
                LignePanier newLine = new LignePanier();
                newLine.setProduit(produitManaged);
                newLine.setQuantite(1);
                newLine.setPrixUnitaire(produitManaged.getPrix());
                newLine.setSousTotal(produitManaged.getPrix());

                // Établir la relation bidirectionnelle
                newLine.setPanier(panier);
                panier.getLignesPanier().add(newLine);

                // Pas besoin de persist() sur newLine grâce à cascade=ALL sur Panier.lignesPanier


            // --- 4. MISE À JOUR DU MONTANT TOTAL DU PANIER ---

// Calcul   du montant à ajouter : Prix du produit * Quantité (1)
            BigDecimal montantLigne = produit.getPrix().multiply(new BigDecimal(newLine.getQuantite()));

// Mise à   jour du total : Ancien total + Montant de la ligne
            panier.setMontantTotal(panier.getMontantTotal().add(montantLigne));

            entityManager.flush();

            System.out.println("SUCCESS: Produit ajouté/mis à jour dans le panier du client " + clientManaged.getEmail());

            // --- 5. REDIRECTION VERS LE PANIER ---
            return "/panier?faces-redirect=true";

        } catch (Exception e) {
            System.err.println("ERREUR LORS DE L'AJOUT AU PANIER: " + e.getMessage());
            e.printStackTrace();
            // Gérer l'erreur utilisateur
            return null;
        }
    }

    public List<Produit> getProduits() {
        return produits;
    }

    public void setProduits(List<Produit> produits) {
        this.produits = produits;
    }

    public Client getClientConnecte() {
        return clientConnecte;
    }

    public void setClientConnecte(Client clientConnecte) {
        this.clientConnecte = clientConnecte;
    }
}