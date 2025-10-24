package org.example.ecommerce.beans;

import jakarta.ejb.Stateless;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.ecommerce.entities.Client;
import org.example.ecommerce.entities.Panier;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Named("registerBean")
@Stateless
public class RegisterBean implements Serializable {

    @PersistenceContext(unitName = "ecommercePU")
    private EntityManager entityManager;

    private Client client = new Client();
    private String confirmPassword;



    public String register() {
        try {
            // Vérifier si les mots de passe correspondent
            if (!client.getPassword().equals(confirmPassword)) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Les mots de passe ne correspondent pas");
                return null;
            }

            System.out.println(client.getNom());
            System.out.println(client.getEmail());

            // Vérifier si l'email existe déjà
            Long count = entityManager.createQuery(
                            "SELECT COUNT(c) FROM Client c WHERE c.email = :email", Long.class)
//                    .setParameter("email", client.getEmail())
                    .getSingleResult();

            if (count > 0) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Cet email est déjà utilisé");
                return null;
            }




            // Persister le client
            entityManager.persist(client);

            Panier panier = new Panier();
            panier.setClient(client);
            panier.setDateCreation(new Date());
            panier.setMontantTotal(BigDecimal.ZERO);
            entityManager.persist(panier);
            client.setPanier(panier);

            System.out.println(client.getPanier());
            System.out.println(client.getId());



            addMessage(FacesMessage.SEVERITY_INFO, "Inscription réussie ! Vous pouvez vous connecter.");

            // Rediriger vers la page de connexion
            return "/login?faces-redirect=true";

        } catch (Exception e) {
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur lors de l'inscription");
            return null;
        }
    }

    private void addMessage(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severity, message, null));
    }

    // Getters et Setters
    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}