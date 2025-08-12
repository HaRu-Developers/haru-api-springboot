package com.haru.api.domain.workspace.service;

import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;

import java.time.LocalDate;

public interface WorkspaceQueryService {

    WorkspaceResponseDTO.DocumentList getDocuments(User user, Workspace workspace, String title);

    WorkspaceResponseDTO.DocumentSidebarList getDocumentWithoutLastOpenedList(User user, Workspace workspace);

    WorkspaceResponseDTO.DocumentCalendarList getDocumentForCalendar(User user, Workspace workspace, LocalDate startDate, LocalDate endDate);

    WorkspaceResponseDTO.WorkspaceEditPage getEditPage(User user, Workspace workspace);
}
