package com.mealio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealio.exception.ResourceNotFoundException;
import com.mealio.model.entity.AuditTrail;
import com.mealio.model.entity.Member;
import com.mealio.repository.AuditTrailRepository;
import com.mealio.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditTrailRepository auditTrailRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void logChange(UUID adminId, String entityType, UUID entityId,
            Object oldValue, Object newValue, String reason) {
        Member admin = memberRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Member (admin)", adminId));

        AuditTrail trail = AuditTrail.builder()
                .admin(admin)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(toJson(oldValue))
                .newValue(toJson(newValue))
                .reason(reason)
                .build();

        auditTrailRepository.save(trail);
        log.info("Audit: {} corrected {} [{}]", admin.getName(), entityType, entityId);
    }

    @Transactional(readOnly = true)
    public List<AuditTrail> getHistory(String entityType, UUID entityId) {
        return auditTrailRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    private String toJson(Object obj) {
        if (obj == null)
            return "null";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}
