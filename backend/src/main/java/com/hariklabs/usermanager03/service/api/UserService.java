package com.hariklabs.usermanager03.service.api;

import com.hariklabs.usermanager03.service.model.UserInput;
import com.hariklabs.usermanager03.service.model.UserQuery;
import com.hariklabs.usermanager03.service.model.UserResult;
import com.hariklabs.usermanager03.service.model.UserResults;

public interface UserService {
    UserResult get(String id);
    UserResults list(UserQuery query);
    UserResult create(UserInput input);
    UserResult update(String id, UserInput input);
    void deactivate(String id);
}
