package org.example.ecommerce.beans;

import jakarta.ejb.Stateless;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.example.ecommerce.entities.Client;

@Named("LoginBean")
@Stateless
public class LoginBean {

    @PersistenceContext(unitName = "ecommercePU")
    private EntityManager entityManager;

    private Client client = new Client();


    public String login() {

        FacesContext facesContext = FacesContext.getCurrentInstance();

        // 1. Début de l'opération de log-in
        System.out.println("--- TENTATIVE DE CONNEXION pour l'email : " + client.getEmail() + " ---");

        // Assurez-vous d'avoir une méthode addMessage dans votre bean pour afficher les messages JSF.

        try {
            // 2. Construire la requête pour vérifier l'email et le mot de passe
            Client foundClient = entityManager.createQuery(
                            "SELECT c FROM Client c WHERE c.email = :email AND c.password = :password",
                            Client.class)
                    .setParameter("email", client.getEmail())
                    .setParameter("password", client.getPassword())
                    .getSingleResult();

            // 3. Succès : Le client a été trouvé (getSingleResult n'a pas levé d'exception)

            facesContext.getExternalContext().getSessionMap().put("loggedUser", foundClient);

            // Afficher le message de succès
            addMessage(FacesMessage.SEVERITY_INFO, "Connexion réussie ! Bienvenue.");
            System.out.println("SUCCESS: Client " + foundClient.getEmail() + " connecté.");

            // Rediriger vers la page d'accueil (ou le tableau de bord)
            return "/index?faces-redirect=true";

        } catch (NoResultException e) {
            // 4. Échec : Aucun client trouvé (email ou mot de passe incorrect)

            // Afficher le message d'erreur
            addMessage(FacesMessage.SEVERITY_ERROR, "Informations de connexion incorrectes. Veuillez réessayer.");
            System.out.println("FAILURE: Email ou mot de passe incorrect pour " + client.getEmail());

            // Rester sur la page de connexion
            return null;

        } catch (Exception e) {
            // 5. Autre Erreur (par exemple, problème de base de données, plusieurs résultats)
            e.printStackTrace();

            // Afficher un message d'erreur générique
            addMessage(FacesMessage.SEVERITY_FATAL, "Une erreur inattendue est survenue lors de la connexion.");
            System.out.println("FATAL ERROR: " + e.getMessage());

            return null;
        }
    }

    private void addMessage(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severity, message, null));
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

}
