package com.vidacotidiana.visionboard.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VisionBoardRepository extends JpaRepository<VisionBoard, UUID> {

    Page<VisionBoard> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
}
