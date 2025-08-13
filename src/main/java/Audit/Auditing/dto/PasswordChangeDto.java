package Audit.Auditing.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeDto {

    @NotEmpty(message = "Password lama tidak boleh kosong.")
    private String oldPassword;

    @NotEmpty(message = "Password baru tidak boleh kosong.")
    @Size(min = 8, message = "Password baru minimal harus 6 karakter.")
    private String newPassword;

    private String confirmPassword;

}
