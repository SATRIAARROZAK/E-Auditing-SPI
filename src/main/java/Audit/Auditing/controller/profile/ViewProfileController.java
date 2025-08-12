// package Audit.Auditing.controller.profile;

// import Audit.Auditing.model.User;
// import Audit.Auditing.service.UserService;
// import org.springframework.security.core.Authentication;
// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;


// public class ViewProfileController {
//     private final UserService userService;

//     public ViewProfileController(UserService userService) {
//         this.userService = userService;
//     }

//     // ✅ TAMBAHKAN METHOD BARU INI
//     @GetMapping
//     public String profilePage(Authentication authentication) {
//         String username = authentication.getName();
//         User user = userService.findByUsername(username);

//         // Cek apakah profil sudah lengkap atau belum
//         if (user.getFullName() == null || user.getFullName().trim().isEmpty() ||
//             user.getPosition() == null || user.getPosition().trim().isEmpty()) {
            
//             // Jika belum lengkap, arahkan ke halaman validasi
//             return "redirect:/profile/validate";
//         } else {
//             // Jika sudah lengkap, arahkan ke halaman lihat profil
//             return "redirect:/profile/view";
//         }
//     }

//     // Method baru untuk secara eksplisit menampilkan halaman view-profile
//     @GetMapping("/view")
//     public String viewProfile(Model model, Authentication authentication) {
//         String username = authentication.getName();
//         User user = userService.findByUsername(username);
//         model.addAttribute("user", user);
//         return "view-profile"; // Menampilkan file view-profile.html
//     }


//     // Method untuk menampilkan form edit (sudah ada sebelumnya)
//     @GetMapping("/edit")
//     public String showEditProfileForm(Model model, Authentication authentication) {
//         // ... (kode yang sudah ada, tidak perlu diubah)
//     }
// }
