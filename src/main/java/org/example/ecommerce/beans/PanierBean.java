package org.example.ecommerce.beans;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.ecommerce.entities.Client;
import org.example.ecommerce.entities.Commande;
import org.example.ecommerce.entities.Panier;
import org.example.ecommerce.entities.LignePanier;
import org.example.ecommerce.services.CommandeService;
import org.example.ecommerce.services.PanierService;

import java.io.Serializable;
import java.util.List;

@Named("panierBean")
@ViewScoped
public class PanierBean implements Serializable {

    @PersistenceContext(unitName = "ecommercePU")
    private EntityManager entityManager;

    @Inject
    private PanierService panierService;

    @Inject
    private CommandeService  commandeService;

    private Panier panier;

    // --- LOGIQUE DE CHARGEMENT ET DE REDIRECTION ---

    @PostConstruct
    public void init() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Client loggedUser = (Client) facesContext.getExternalContext().getSessionMap().get("loggedUser");

        if (loggedUser == null) {
            // Utilisateur non connecté : Redirection vers la page de login
            try {
                facesContext.getExternalContext().redirect(
                        facesContext.getExternalContext().getRequestContextPath() + "/login.xhtml?faces-redirect=true");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // Récupérer le client "managed" pour accéder au panier
        Client clientManaged = entityManager.find(Client.class, loggedUser.getId());

        if (clientManaged != null) {
            // Charger le panier et ses lignes (qui sont déjà EAGER)
            this.panier = clientManaged.getPanier();
        }
    }

    // --- ACTIONS DU PANIER ---

    public String supprimerLigne(LignePanier ligne) {
        if (panier != null) {
            panierService.supprimerLigne(panier.getId() , ligne.getId());
        }

        entityManager.clear();// on fait ca pour vider complètement le contexte de persistance

        // puis les instruction suivante pour poser des nouveaux infos dans le contexte


        Client loggedUserInSession = (Client) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("loggedUser");

        Client clientRecharged = entityManager.find(Client.class, loggedUserInSession.getId());

        // Mettre à jour la propriété du bean avec le panier rechargé
        this.panier = clientRecharged.getPanier();

        // En cas de suppression de la dernière ligne, ceci forcera le rafraîchissement des conditions 'rendered'
        FacesContext.getCurrentInstance().addMessage(null,
                new jakarta.faces.application.FacesMessage(
                        jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                        "Produit retiré du panier.", null));
        return null;
    }

    public String passerCommande() {

        if (panier == null || panier.getLignesPanier().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new jakarta.faces.application.FacesMessage(
                            jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                            "Votre panier est vide et ne peut pas être commandé.", null));
            return null; // Reste sur la même page
        }

        // Récupérer le Client stocké dans la session (doit être non nul à ce stade)
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Client loggedUser = (Client) facesContext.getExternalContext().getSessionMap().get("loggedUser");

        if (loggedUser == null) {
            // Sécurité supplémentaire: si la session a expiré, rediriger
            return "/public/login?faces-redirect=true";
        }

        try {
            // Déléguer toute la logique de création et de persistance au Service EJB
            commandeService.creerEtCommander(loggedUser, panier);

            // Optionnel: Ajouter un message de succès
            facesContext.addMessage(null,
                    new jakarta.faces.application.FacesMessage(
                            jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                            "Votre commande a été passée avec succès!", null));

            // Redirection vers la page d'historique des commandes
            return "commandes?faces-redirect=true";

        } catch (Exception e) {
            System.err.println("Erreur lors du passage de commande: " + e.getMessage());
            facesContext.addMessage(null,
                    new jakarta.faces.application.FacesMessage(
                            jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                            "Une erreur est survenue lors du passage de votre commande.", null));
            return null; // Reste sur la page du panier en cas d'erreur
        }
    }

    private void recalculerMontantTotal() {
        if (panier == null) return;
        java.math.BigDecimal total = panier.getLignesPanier().stream()
                .map(LignePanier::getSousTotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        panier.setMontantTotal(total);
    }

    // --- GETTERS et SETTERS ---

    public Panier getPanier() {
        return panier;
    }
    // Pas besoin de setPanier() ici

    public List<LignePanier> getLignesPanier() {
        return panier != null ? panier.getLignesPanier() : java.util.Collections.emptyList();
    }
}