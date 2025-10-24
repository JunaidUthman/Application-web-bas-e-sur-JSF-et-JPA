package org.example.ecommerce.beans;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.ecommerce.entities.Client;
import org.example.ecommerce.entities.Commande;
import org.example.ecommerce.services.CommandeService;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Named("commandeBean") // Renommé en minuscules pour la convention EL
@ViewScoped
public class CommandeBean implements Serializable { // Doit être Serializable pour @ViewScoped

    @Inject
    private CommandeService commandeService;

    // CORRECTION : Doit être une liste car un client a plusieurs commandes.
    private List<Commande> commandes;

    @PersistenceContext(unitName = "ecommercePU")
    private EntityManager entityManager;


    @PostConstruct
    public void init(){
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Client loggedUser = (Client) facesContext.getExternalContext().getSessionMap().get("loggedUser");

        if (loggedUser == null) {
            // Utilisateur non connecté : Redirection vers la page de login
            try {
                facesContext.getExternalContext().redirect(
                        facesContext.getExternalContext().getRequestContextPath() + "/public/login.xhtml?faces-redirect=true");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // 1. Récupérer le client "managed"
        // CORRECTION MAJEURE : On cherche un Client, pas une Commande.
        Client clientManaged = entityManager.find(Client.class, loggedUser.getId());

        if (clientManaged != null) {

            try {
                this.commandes = entityManager.createQuery(
                                "SELECT c FROM Commande c WHERE c.client.id = :clientId ORDER BY c.dateCommande DESC",
                                Commande.class)
                        .setParameter("clientId", clientManaged.getId())
                        .getResultList();
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement des commandes: " + e.getMessage());
                this.commandes = Collections.emptyList(); // Initialiser à vide en cas d'erreur
            }
        } else {
            this.commandes = Collections.emptyList();
        }
    }

    public String annulerCommande(Commande commande){
        commandeService.supprimerCommande(commande);
        return "commandes.xhtml?faces-redirect=true";
    }



    // Ajout d'un getter pour l'affichage dans la vue
    public List<Commande> getCommandes() {
        return commandes;
    }

    // Vous aurez besoin d'autres méthodes (ex: annulerCommande) plus tard...
}