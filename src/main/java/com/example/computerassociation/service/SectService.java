package com.example.computerassociation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.computerassociation.entity.AuditLog;
import com.example.computerassociation.entity.Disciple;

import java.util.List;

public interface SectService extends IService<Disciple> {

    List<Disciple> getAllDisciples();

    List<Disciple> getManagementDisciples();

    List<Disciple> getDisciplesByPeak(String peak);

    List<Disciple> searchDisciples(String keyword);

    Disciple getDiscipleById(Long id);

    Disciple addDisciple(Disciple disciple);

    boolean updateDisciple(Disciple disciple);

    boolean deleteDisciple(Long id, String reason);

    boolean moveDisciplePeak(Long discipleId, String newPeak);

    List<Disciple> filterDisciplesByPeak(String peak);

    List<AuditLog> getDiscipleHistory(Long discipleId);

    List<AuditLog> getAllDiscipleHistory(int page, int size);
}
