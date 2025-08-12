package Audit.Auditing.controller.profile;

import Audit.Auditing.dto.ProfileDto;
import Audit.Auditing.model.User;
import Audit.Auditing.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    // Gerbang utama saat menu profil di klik
    @GetMapping
    public String profilePage(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        // Cek apakah profil sudah lengkap atau belum
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            // Jika belum, arahkan ke halaman validasi
            return "redirect:/profile/validate";
        } else {
            // Jika sudah, arahkan ke halaman lihat profil
            return "redirect:/profile/view";
        }
    }
    
    // Menampilkan halaman lihat profil (read-only)
    @GetMapping("/view")
    public String viewProfile(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        return "pages/account/view-profile";
    }

    // Menampilkan halaman untuk melengkapi profil pertama kali
    @GetMapping("/validate")
    public String showValidateProfileForm(Model model, Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("Authentication object is null");
        }
        String username = authentication.getName();
        User user = userService.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        return "pages/account/validate-profile";
    }
    
    // Memproses data dari form validasi profil
    @PostMapping("/validate")
    public String processValidateProfile(@ModelAttribute("user") ProfileDto profileDto,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        userService.completeProfile(username, profileDto);
        redirectAttributes.addFlashAttribute("successMessage", "Profil berhasil dilengkapi!");
        return "redirect:/profile/view";
    }

    // Menampilkan halaman untuk mengedit profil yang sudah ada
    @GetMapping("/edit")
    public String showEditProfileForm(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        
        ProfileDto profileDto = new ProfileDto();
        profileDto.setFullName(user.getFullName());
        profileDto.setPosition(user.getPosition());
        profileDto.setPhoneNumber(user.getPhoneNumber());

        model.addAttribute("profileDto", profileDto);
        model.addAttribute("currentPhoto", user.getPhoto());
        return "edit-profile";
    }
    
    // Memproses data dari form edit profil
    @PostMapping("/update")
     public String updateProfile(@Validated @ModelAttribute("profileDto") ProfileDto profileDto,
                                BindingResult result,
                                Principal principal,
                                RedirectAttributes redirectAttributes,
                                Model model) {

        if (result.hasErrors()) {
            // Jika ada error validasi, muat ulang user untuk ditampilkan di view
            User user = userService.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                model.addAttribute("user", user);
                model.addAttribute("isFirstTime", !user.isProfileComplete());
            }
            return "pages/account/validate-profile";
        }
        try {
            userService.updateProfile(principal.getName(), profileDto);
            redirectAttributes.addFlashAttribute("successMessage", "Profil berhasil diperbarui!");
            // Setelah profil lengkap, arahkan ke dashboard
            return "redirect:/dashboard";
        } catch (Exception e) {
            e.printStackTrace(); // Log the exception
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal memperbarui profil: " + e.getMessage());
            return "redirect:/profile/edit";
        }
    }
}