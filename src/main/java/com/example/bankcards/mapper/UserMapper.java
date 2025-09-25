package com.example.bankcards.mapper;

import com.example.bankcards.dto.response.UserDTOResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.RoleName;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    UserDTOResponse userToUserDTOResponse(User user);

    default RoleName map(Role role) {
        return role != null ? role.getName() : null;
    }
}
