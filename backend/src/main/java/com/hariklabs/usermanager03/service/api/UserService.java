package com.hariklabs.usermanager03.service.api;

import com.hariklabs.usermanager03.service.model.UserInput;
import com.hariklabs.usermanager03.service.model.UserQuery;
import com.hariklabs.usermanager03.service.model.UserResult;
import com.hariklabs.usermanager03.service.model.UserResults;
import java.util.UUID;

public interface UserService {
    UserResult get(UUID id);
    UserResults list(UserQuery query);
    UserResult create(UserInput input);
    UserResult update(UUID id, UserInput input);
    void deactivate(UUID id);
}
