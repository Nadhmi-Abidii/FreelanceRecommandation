package com.towork.user.service.impl;

import com.towork.exception.ResourceNotFoundException;
import com.towork.user.entity.Client;
import com.towork.user.dto.ClientDto;
import com.towork.user.repository.ClientRepository;
import com.towork.user.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Override
    public Client createClient(Client client) {
        return clientRepository.save(client);
    }

    /**
     * Mise à jour "admin" générique :
     * - conserve createdAt / id
     * - ne remplace que les champs non nuls / non vides pour les String
     * - évite d’écraser email/password si non fournis
     */
    @Override
    public Client updateClient(Long id, Client incoming) {
        Client existing = getClientById(id);

        // Champs identifiants conservés
        incoming.setId(existing.getId());
        incoming.setCreatedAt(existing.getCreatedAt());

        // Fusion champ par champ
        copyIfNotBlank(incoming.getFirstName(), existing::setFirstName);
        copyIfNotBlank(incoming.getLastName(),  existing::setLastName);
        copyIfNotBlank(incoming.getEmail(),     existing::setEmail);
        copyIfNotBlank(incoming.getPassword(),  existing::setPassword);
        copyIfNotBlank(incoming.getPhone(),     existing::setPhone);

        copyIfNotBlank(incoming.getCompanyName(), existing::setCompanyName);
        copyIfNotBlank(incoming.getCompanySize(), existing::setCompanySize);
        copyIfNotBlank(incoming.getIndustry(),    existing::setIndustry);
        copyIfNotBlank(incoming.getWebsite(),     existing::setWebsite);

        copyIfNotBlank(incoming.getAddress(),     existing::setAddress);
        copyIfNotBlank(incoming.getCity(),        existing::setCity);
        copyIfNotBlank(incoming.getCountry(),     existing::setCountry);
        copyIfNotBlank(incoming.getPostalCode(),  existing::setPostalCode);

        copyIfNotBlank(incoming.getProfilePicture(), existing::setProfilePicture);
        copyIfNotBlank(incoming.getBio(),            existing::setBio);

        // Flags (si vous souhaitez permettre la modif)
        if (incoming.getIsVerified() != null) {
            existing.setIsVerified(incoming.getIsVerified());
        }
        if (incoming.getIsActive() != null) {
            existing.setIsActive(incoming.getIsActive());
        }

        return clientRepository.save(existing);
    }

    @Override
    public void deleteClient(Long id) {
       Client existing = getClientById(id);
    clientRepository.delete(existing);
    }

    @Override
    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
    }

    @Override
    public Client getClientByEmail(String email) {
        return clientRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with email: " + email));
    }

    @Override
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    @Override
    public Page<Client> getAllClients(Pageable pageable) {
        return clientRepository.findAll(pageable);
    }

    @Override
    public List<Client> getVerifiedClients() {
        return clientRepository.findByIsVerified(true);
    }

    @Override
    public List<Client> searchClients(String keyword) {
        return clientRepository.findByCompanyNameContainingIgnoreCase(keyword);
    }

    @Override
    public ClientDto convertToDto(Client client) {
        return new ClientDto(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getEmail(),
                client.getPhone(),
                client.getCompanyName(),
                client.getCompanySize(),
                client.getIndustry(),
                client.getWebsite(),
                client.getAddress(),
                client.getCity(),
                client.getCountry(),
                client.getPostalCode(),
                client.getProfilePicture(),
                client.getBio(),
                client.getIsVerified(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }

    @Override
    public Client convertToEntity(ClientDto clientDto) {
        Client client = new Client();
        client.setId(clientDto.getId());
        client.setFirstName(clientDto.getFirstName());
        client.setLastName(clientDto.getLastName());
        client.setEmail(clientDto.getEmail());
        client.setPhone(clientDto.getPhone());
        client.setCompanyName(clientDto.getCompanyName());
        client.setCompanySize(clientDto.getCompanySize());
        client.setIndustry(clientDto.getIndustry());
        client.setWebsite(clientDto.getWebsite());
        client.setAddress(clientDto.getAddress());
        client.setCity(clientDto.getCity());
        client.setCountry(clientDto.getCountry());
        client.setPostalCode(clientDto.getPostalCode());
        client.setProfilePicture(clientDto.getProfilePicture());
        client.setBio(clientDto.getBio());
        client.setIsVerified(clientDto.getIsVerified());
        return client;
    }

    @Override
    public Client verifyClient(Long id) {
        Client client = getClientById(id);
        client.setIsVerified(true);
        return clientRepository.save(client);
    }

    /**
     * Mise à jour PROFIL pour l’utilisateur connecté :
     * - ne touche jamais à email/password ici
     * - met à jour seulement les champs fournis (non nuls / non vides)
     */
    @Override
    public Client updateClientProfile(Long id, Client incoming) {
        Client existing = getClientById(id);

        copyIfNotBlank(incoming.getFirstName(), existing::setFirstName);
        copyIfNotBlank(incoming.getLastName(),  existing::setLastName);
        copyIfNotBlank(incoming.getPhone(),     existing::setPhone);

        copyIfNotBlank(incoming.getCompanyName(), existing::setCompanyName);
        copyIfNotBlank(incoming.getCompanySize(), existing::setCompanySize);
        copyIfNotBlank(incoming.getIndustry(),    existing::setIndustry);
        copyIfNotBlank(incoming.getWebsite(),     existing::setWebsite);

        copyIfNotBlank(incoming.getAddress(),     existing::setAddress);
        copyIfNotBlank(incoming.getCity(),        existing::setCity);
        copyIfNotBlank(incoming.getCountry(),     existing::setCountry);
        copyIfNotBlank(incoming.getPostalCode(),  existing::setPostalCode);

        copyIfNotBlank(incoming.getProfilePicture(), existing::setProfilePicture);
        copyIfNotBlank(incoming.getBio(),            existing::setBio);

        // on ne modifie pas email / password ici
        return clientRepository.save(existing);
    }

    /* ========= Helpers ========= */

    private void copyIfNotBlank(String value, Consumer<String> setter) {
        if (value != null && !value.isBlank()) {
            setter.accept(value);
        }
    }
}
