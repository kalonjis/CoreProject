package be.steby.CoreProject.bll.specifications;

import be.steby.CoreProject.dl.entities.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> searchInAllFields(String searchTerm) {
        return (root, query, builder) -> {
            if (searchTerm == null || searchTerm.isEmpty()) {
                return null;
            }

            String likePattern = "%" + searchTerm.toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("username")), likePattern),
                    builder.like(builder.lower(root.get("firstname")), likePattern),
                    builder.like(builder.lower(root.get("lastname")), likePattern),
                    builder.like(builder.lower(root.get("email")), likePattern)
            );
        };
    }


    public static Specification<User> searchByCriteria(String username, String firstname, String lastname, String email, String phoneNumber) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Vérifie chaque champ et ajoute un predicate si le champ est renseigné
            if (username != null && !username.isEmpty()) {
                String usernamePattern = "%" + username.toLowerCase() + "%";
                predicates.add(builder.like(builder.lower(root.get("username")), usernamePattern));
            }

            if (firstname != null && !firstname.isEmpty()) {
                String firstnamePattern = "%" + firstname.toLowerCase() + "%";
                predicates.add(builder.like(builder.lower(root.get("firstname")), firstnamePattern));
            }

            if (lastname != null && !lastname.isEmpty()) {
                String lastnamePattern = "%" + lastname.toLowerCase() + "%";
                predicates.add(builder.like(builder.lower(root.get("lastname")), lastnamePattern));
            }

            if (email != null && !email.isEmpty()) {
                String emailPattern = "%" + email.toLowerCase() + "%";
                predicates.add(builder.like(builder.lower(root.get("email")), emailPattern));
            }

            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                String phoneNumberPattern = "%" + phoneNumber + "%";
                predicates.add(builder.like(builder.lower(root.get("phoneNumber")), phoneNumberPattern));
            }

            // Si aucun critère n'est renseigné, retourner null (pas de filtre)
            if (predicates.isEmpty()) {
                return null;
            }

            return builder.and(predicates.toArray(new Predicate[0])); // Combine les predicates
        };
    }

}
