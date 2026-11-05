package com.hariklabs.usermanager03.service.api;

import com.hariklabs.usermanager03.service.api.dto.UserInputDTO;
import com.hariklabs.usermanager03.service.api.dto.UserQueryDTO;
import com.hariklabs.usermanager03.service.api.dto.UserResultDTO;
import com.hariklabs.usermanager03.service.api.dto.UserResultsDTO;
import java.util.UUID;

public interface UserService {
    UserResultDTO get(UUID id);
    UserResultsDTO list(UserQueryDTO query);
    UserResultDTO create(UserInputDTO input);
    UserResultDTO update(UUID id, UserInputDTO input);
    void deactivate(UUID id);
}
