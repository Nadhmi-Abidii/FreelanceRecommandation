package com.towork.user.service;

import com.towork.user.entity.Client;
import com.towork.user.dto.ClientDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClientService {
    Client createClient(Client client);
    Client updateClient(Long id, Client client);
    void deleteClient(Long id);
    Client getClientById(Long id);
    Client getClientByEmail(String email);
    List<Client> getAllClients();
    Page<Client> getAllClients(Pageable pageable);
    List<Client> getVerifiedClients();
    List<Client> searchClients(String keyword);
    ClientDto convertToDto(Client client);
    Client convertToEntity(ClientDto clientDto);
    Client verifyClient(Long id);
    Client updateClientProfile(Long id, Client client);
}
