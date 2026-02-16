package com.towork.user.service.impl;

import com.towork.exception.ResourceNotFoundException;
import com.towork.user.entity.Domaine;
import com.towork.user.dto.DomaineDto;
import com.towork.user.repository.DomaineRepository;
import com.towork.user.service.DomaineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DomaineServiceImpl implements DomaineService {

    private final DomaineRepository domaineRepository;

    @Override
    public Domaine createDomaine(Domaine domaine) {
        return domaineRepository.save(domaine);
    }

    @Override
    public Domaine updateDomaine(Long id, Domaine domaine) {
        Domaine existingDomaine = getDomaineById(id);
        domaine.setId(existingDomaine.getId());
        domaine.setCreatedAt(existingDomaine.getCreatedAt());
        return domaineRepository.save(domaine);
    }

    @Override
    public void deleteDomaine(Long id) {
        Domaine domaine = getDomaineById(id);
        domaine.setIsActive(false);
        domaineRepository.save(domaine);
    }

    @Override
    public Domaine getDomaineById(Long id) {
        return domaineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domaine not found with id: " + id));
    }

    @Override
    public Domaine getDomaineByName(String name) {
        return domaineRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Domaine not found with name: " + name));
    }

    @Override
    public List<Domaine> getAllDomaines() {
        return domaineRepository.findAll();
    }

    @Override
    public Page<Domaine> getAllDomaines(Pageable pageable) {
        return domaineRepository.findAll(pageable);
    }

    @Override
    public List<Domaine> getActiveDomaines() {
        return domaineRepository.findActiveDomainesOrderByName();
    }

    @Override
    public List<Domaine> searchDomaines(String keyword) {
        return domaineRepository.findByNameContainingIgnoreCase(keyword);
    }

    @Override
    public DomaineDto convertToDto(Domaine domaine) {
        return new DomaineDto(
                domaine.getId(),
                domaine.getName(),
                domaine.getDescription(),
                domaine.getIcon(),
                domaine.getColor(),
                domaine.getIsActive(),
                domaine.getCreatedAt(),
                domaine.getUpdatedAt()
        );
    }

    @Override
    public Domaine convertToEntity(DomaineDto domaineDto) {
        Domaine domaine = new Domaine();
        domaine.setId(domaineDto.getId());
        domaine.setName(domaineDto.getName());
        domaine.setDescription(domaineDto.getDescription());
        domaine.setIcon(domaineDto.getIcon());
        domaine.setColor(domaineDto.getColor());
        domaine.setIsActive(domaineDto.getIsActive());
        return domaine;
    }

    @Override
    public Domaine activateDomaine(Long id) {
        Domaine domaine = getDomaineById(id);
        domaine.setIsActive(true);
        return domaineRepository.save(domaine);
    }

    @Override
    public Domaine deactivateDomaine(Long id) {
        Domaine domaine = getDomaineById(id);
        domaine.setIsActive(false);
        return domaineRepository.save(domaine);
    }
}
