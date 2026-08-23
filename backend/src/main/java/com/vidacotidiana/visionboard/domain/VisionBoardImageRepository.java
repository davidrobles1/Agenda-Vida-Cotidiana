package com.vidacotidiana.visionboard.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VisionBoardImageRepository extends JpaRepository<VisionBoardImage, UUID> {
}
