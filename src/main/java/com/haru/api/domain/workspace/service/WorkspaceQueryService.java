package com.haru.api.domain.workspace.service;

import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;

import java.time.LocalDate;

public interface WorkspaceQueryService {

    WorkspaceResponseDTO.DocumentList getDocuments(Long userId, Long workspaceId, String title);

    WorkspaceResponseDTO.DocumentSidebarList getDocumentWithoutLastOpenedList(Long userId, Long workspaceId);

    WorkspaceResponseDTO.DocumentCalendarList getDocumentForCalendar(Long userId, Long workspaceId, LocalDate startDate, LocalDate endDate);

    WorkspaceResponseDTO.WorkspaceEditPage getEditPage(Long userId, String workspaceId);
}
