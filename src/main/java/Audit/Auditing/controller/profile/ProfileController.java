package Audit.Auditing.controller.profile;

import Audit.Auditing.dto.PasswordChangeDto;
import Audit.Auditing.dto.ProfileDto;
import Audit.Auditing.model.User;
import Audit.Auditing.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.validation.annotation.Validated;

import java.io.IOException;

// Mengelompokkan semua URL di bawah /profile
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Metode ini adalah "Gerbang Utama" saat menu profil di-klik.
     * Ia akan memeriksa kelengkapan profil dan mengarahkan pengguna ke halaman yang
     * tepat.
     */
    @GetMapping
    public String mainProfilePage(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Cek jika salah satu data wajib masih kosong
        if (user.getFullName() == null || user.getFullName().trim().isEmpty() ||
                user.getPosition() == null || user.getPosition().trim().isEmpty()) {

            // Jika BELUM LENGKAP, paksa ke halaman validasi
            return "redirect:/profile/edit";
        } else {
            // Jika SUDAH LENGKAP, arahkan ke halaman untuk melihat profil
            return "redirect:/profile/view";
        }
    }

    /**
     * Menampilkan halaman untuk MELIHAT profil yang sudah lengkap (read-only).
     */
    @GetMapping("/view")
    public String viewProfile(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("user", user);
        return "pages/account/view-profile"; // Mengarah ke view-profile.html
    }

    @PostMapping("/update")
    public String updateProfile(@Valid @ModelAttribute("profileDto") ProfileDto profileDto,
            BindingResult result,
            @RequestParam("photoFile") MultipartFile photoFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) throws IOException {
        String username = authentication.getName();
        if (result.hasErrors()) {
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            model.addAttribute("user", user);
            return "pages/account/edit-profile";
        }

        profileDto.setPhoto(photoFile);

        // [TETAP] Memanggil method untuk memperbarui profil
        userService.updateProfile(username, profileDto);

        redirectAttributes.addFlashAttribute("successMessage", "Profil berhasil diperbarui!");

        // [TUJUAN] Mengarahkan kembali ke halaman lihat profil
        return "redirect:/profile/view";
    }

    /**
     * Menampilkan halaman untuk MENGEDIT profil yang sudah ada.
     */
    @GetMapping("/edit")
    public String showEditProfileForm(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Buat DTO dan isi dengan data yang sudah ada
        ProfileDto profileDto = new ProfileDto();
        profileDto.setFullName(user.getFullName());
        profileDto.setPosition(user.getPosition());
        profileDto.setPhoneNumber(user.getPhoneNumber());
        profileDto.setAddress(user.getAddress());
        profileDto.setEmail(user.getEmail());
        profileDto.setUsername(user.getUsername());

        model.addAttribute("user", user);

        model.addAttribute("profileDto", profileDto);
        model.addAttribute("currentPhoto", user.getPhotoPath());
        model.addAttribute("passwordChangeDto", new PasswordChangeDto());
        model.addAttribute("isProfileComplete", user.isProfileComplete());

        return "pages/account/edit-profile"; // Mengarah ke edit-profile.html
    }

    /**
     * Memproses permintaan ganti password dari modal.
     */
    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordChangeDto") PasswordChangeDto passwordDto,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (!passwordDto.getNewPassword().equals(passwordDto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.passwordChangeDto", "Konfirmasi password baru tidak cocok.");
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("passwordError",
                    "Gagal mengubah password. Periksa kembali input Anda.");
            return "redirect:/profile/edit";
        }

        try {
            String username = authentication.getName();
            userService.changePassword(username, passwordDto.getOldPassword(), passwordDto.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Password berhasil diubah!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
            return "redirect:/profile/edit";
        }

        return "redirect:/profile/view";
    }
}

/**
 * Menampilkan halaman untuk MENGISI profil pertama kali.
 */
// @GetMapping("/validate")
// public String showValidateProfileForm(Model model) {
// // Kirim DTO kosong untuk diisi oleh form
// model.addAttribute("profileDto", new ProfileDto());
// return "pages/account/validate-profile"; // Halaman baru yang terpisah
// }

// @PostMapping("/validate")
// public String processValidateProfile(@Valid @ModelAttribute("profileDto")
// ProfileDto profileDto,
// BindingResult result,
// Authentication authentication,
// RedirectAttributes redirectAttributes,
// Model model) throws IOException {
// if (result.hasErrors()) {
// model.addAttribute("profileDto", profileDto);
// return "pages/account/validate-profile";
// }

// String username = authentication.getName();
// // [DIUBAH] Memanggil method khusus untuk melengkapi profil
// userService.completeProfile(username, profileDto);

// redirectAttributes.addFlashAttribute("successMessage", "Profil berhasil
// dilengkapi! Selamat datang.");

// // [TUJUAN] Mengarahkan ke Dashboard
// return "redirect:/dashboard";
// }

/**
 * Alur EDIT: Memanggil service updateProfile()
 * dan mengarahkan ke /profile/view.
 */

/**
 * Memproses data dari form edit profil.
 */
// @PostMapping("/update")
// public String updateProfile(@Valid @ModelAttribute("profileDto") ProfileDto
// profileDto,
// BindingResult result,
// @RequestParam("photoFile") MultipartFile photoFile,
// Authentication authentication,
// RedirectAttributes redirectAttributes,
// Model model) throws IOException {

// if (result.hasErrors()) {
// String username = authentication.getName();
// User user = userService.findByUsername(username)
// .orElseThrow(() -> new IllegalArgumentException("User not found"));
// model.addAttribute("currentPhoto", user.getPhotoPath());
// return "pages/account/edit-profile";
// }

// // --- INI BAGIAN YANG DIPERBAIKI ---
// // Set file foto yang di-upload ke dalam DTO sebelum dikirim ke service
// profileDto.setPhoto(photoFile);
// // ---------------------------------

// // Panggil service untuk memperbarui profil
// String username = authentication.getName();
// userService.updateProfile(username, profileDto);
// redirectAttributes.addFlashAttribute("successMessage", "Profil berhasil
// diperbarui!");
// return "redirect:/profile/view";
// }

// @PostMapping("/validate")
// public String processValidateProfile(@Valid @ModelAttribute("profileDto")
// ProfileDto profileDto,
// BindingResult result,
// Authentication authentication,
// RedirectAttributes redirectAttributes,
// Model model) throws IOException { // Tambahkan Model
// if (result.hasErrors()) {
// // [DIPERBAIKI] Kirim kembali DTO yang sama agar input pengguna tidak hilang
// model.addAttribute("profileDto", profileDto);
// return "pages/account/validate-profile";
// }

// String username = authentication.getName();
// userService.completeProfile(username, profileDto);

// redirectAttributes.addFlashAttribute("successMessage", "Profil berhasil
// dilengkapi! Selamat datang.");

// return "redirect:/dashboard";
// }

// @PostMapping("/validate")
// public String processValidateProfile(@Valid @ModelAttribute("profileDto")
// ProfileDto profileDto,
// BindingResult result,
// Authentication authentication,
// RedirectAttributes redirectAttributes) {
// if (result.hasErrors()) {
// // Jika ada error, kembali ke form validasi
// return "pages/account/validate-profile";
// }
// String username = authentication.getName();
// // Gunakan method updateProfile yang sudah kita modifikasi
// userService.updateProfile(username, profileDto);
// redirectAttributes.addFlashAttribute("successMessage", "Profil berhasil
// dilengkapi!");
// return "redirect:/dashboard"; // Arahkan ke halaman lihat profil setelah
// selesai
// }