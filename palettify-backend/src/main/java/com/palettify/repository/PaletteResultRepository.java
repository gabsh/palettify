package com.palettify.repository;

import com.palettify.model.PaletteResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaletteResultRepository extends JpaRepository<PaletteResult, Long> {
    Optional<PaletteResult> findByDomain(String domain);
    Page<PaletteResult> findAll(Pageable pageable);
}