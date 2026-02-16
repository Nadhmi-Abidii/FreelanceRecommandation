package com.towork.contract.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import com.towork.contract.entity.Contrat;
import com.towork.contract.entity.EtatContrat;

public interface ContratService {
    Contrat createContrat(Contrat contrat);
    Contrat updateContrat(Long id, Contrat contrat);
    void deleteContrat(Long id);
    Contrat getContratById(Long id);
    List<Contrat> getContratsByClient(Long clientId);
    List<Contrat> getContratsByFreelancer(Long freelancerId);
    List<Contrat> getContratsByEtat(EtatContrat etat);
    Page<Contrat> getAllContrats(Pageable pageable);
    Contrat updateContratEtat(Long id, EtatContrat etat);
    List<Contrat> getActiveContrats();
}
