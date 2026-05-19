package com.projectwork.Smart.Parking.System.dto.request;

import lombok.Data;
//new added
@Data
public class UserProfileUpdateRequestDto {
    private String name;
    private String phone;
    private String currentPassword;
    private String newPassword;
}
