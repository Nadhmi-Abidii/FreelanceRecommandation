package com.towork.user.service;

import com.towork.user.entity.Domaine;
import com.towork.user.dto.DomaineDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DomaineService {
    Domaine createDomaine(Domaine domaine);
    Domaine updateDomaine(Long id, Domaine domaine);
    void deleteDomaine(Long id);
    Domaine getDomaineById(Long id);
    Domaine getDomaineByName(String name);
    List<Domaine> getAllDomaines();
    Page<Domaine> getAllDomaines(Pageable pageable);
    List<Domaine> getActiveDomaines();
    List<Domaine> searchDomaines(String keyword);
    DomaineDto convertToDto(Domaine domaine);
    Domaine convertToEntity(DomaineDto domaineDto);
    Domaine activateDomaine(Long id);
    Domaine deactivateDomaine(Long id);
}
