package Audit.Auditing.service;

import Audit.Auditing.config.CustomUserDetails;
import Audit.Auditing.dto.ProfileDto;
import Audit.Auditing.dto.UserDto;
import Audit.Auditing.model.Role;
import Audit.Auditing.model.User;
import Audit.Auditing.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserDetailsServiceImpl implements UserService, UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + username));

        return new CustomUserDetails(user);
    }

    @Override
    public User saveUser(UserDto userDto) {
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        String roleStr = userDto.getRole().toLowerCase();
        user.setRole(Role.valueOf(roleStr));
        user.setEnabled(true);
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Page<User> searchUsers(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        // Assuming you add a search method to UserRepository, e.g.:
        // @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%',
        // :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        // LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        // Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
        // For simplicity, for now, if no specific search method for users, you might
        // fetch all and filter in memory (not ideal for large datasets)
        // or add the @Query as shown above to your UserRepository.
        return userRepository
                .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(keyword,
                        keyword, keyword, pageable);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());

        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }

        user.setRole(Role.valueOf(userDto.getRole().toLowerCase()));

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public User updateProfile(String username, ProfileDto profileDto) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

        // Simpan foto profil jika ada yang diunggah
        if (profileDto.getPhoto() != null && !profileDto.getPhoto().isEmpty()) {
            String fileName = fileStorageService.storeFile(profileDto.getPhoto());
            user.setPhotoPath(fileName);
        }

        // Simpan tanda tangan jika ada
        if (profileDto.getSignatureDataUrl() != null && !profileDto.getSignatureDataUrl().isEmpty()) {
            String fileName = fileStorageService.storeBase64File(profileDto.getSignatureDataUrl());
            user.setSignaturePath(fileName);
        }

        // Update semua data dari form
        user.setFullName(profileDto.getFullName());
        user.setPosition(profileDto.getPosition());
        user.setPhoneNumber(profileDto.getPhoneNumber());
        user.setAddress(profileDto.getAddress());
        user.setEmail(profileDto.getEmail());
        user.setUsername(profileDto.getUsername());

        // [KUNCI] Tandai profil sebagai "lengkap" setelah pertama kali diisi
        if (!user.isProfileComplete()) {
            user.setProfileComplete(true);
        }

        User updatedUser = userRepository.save(user);

        // Segarkan sesi otentikasi agar data baru langsung terlihat di seluruh aplikasi
        refreshUserSession(updatedUser);

        return updatedUser;
    }

    private void refreshUserSession(User user) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails newUserDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                newUserDetails,
                authentication.getCredentials(),
                newUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

    @Override
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan."));

        // Cek apakah password lama yang dimasukkan benar
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Password lama yang Anda masukkan salah.");
        }

        // Enkripsi dan simpan password baru
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}

// private void refreshUserSession(User user) {
// Authentication authentication =
// SecurityContextHolder.getContext().getAuthentication();
// CustomUserDetails newUserDetails = new CustomUserDetails(user);
// UsernamePasswordAuthenticationToken newAuth = new
// UsernamePasswordAuthenticationToken(
// newUserDetails,
// authentication.getCredentials(),
// newUserDetails.getAuthorities());
// SecurityContextHolder.getContext().setAuthentication(newAuth);
// }

// @Override
// @Transactional
// public User updateProfile(String currentUsername, ProfileDto profileDto) {
// User user = userRepository.findByUsername(currentUsername)
// .orElseThrow(() -> new EntityNotFoundException("User not found with username:
// " + currentUsername));

// // Simpan username lama untuk perbandingan
// String oldUsername = user.getUsername();

// // Proses update foto dan tanda tangan (jika ada)
// if (profileDto.getPhoto() != null && !profileDto.getPhoto().isEmpty()) {
// String fileName = fileStorageService.storeFile(profileDto.getPhoto());
// user.setPhotoPath(fileName);
// }

// if (profileDto.getSignatureImage() != null &&
// !profileDto.getSignatureImage().isEmpty()) {
// String fileName =
// fileStorageService.storeFile(profileDto.getSignatureImage());
// user.setSignaturePath(fileName);
// } else if (profileDto.getSignatureDataUrl() != null &&
// !profileDto.getSignatureDataUrl().isEmpty()) {
// String fileName =
// fileStorageService.storeBase64File(profileDto.getSignatureDataUrl());
// user.setSignaturePath(fileName);
// }

// // Update informasi profil lainnya
// user.setFullName(profileDto.getFullName());
// user.setPosition(profileDto.getPosition());
// user.setPhoneNumber(profileDto.getPhoneNumber());
// user.setAddress(profileDto.getAddress());
// user.setEmail(profileDto.getEmail());
// user.setProfileComplete(true);

// // Ambil username baru dari DTO dan cek apakah berubah
// String newUsername = profileDto.getUsername();
// boolean isUsernameChanged = !oldUsername.equals(newUsername);

// if (isUsernameChanged) {
// user.setUsername(newUsername);
// }

// User updatedUser = userRepository.save(user);

// // // Jika username berubah, perbarui konteks keamanan (sesi)
// // if (isUsernameChanged) {
// // UserDetails newUserDetails = new CustomUserDetails(updatedUser);
// // Authentication newAuth = new
// // UsernamePasswordAuthenticationToken(newUserDetails, null,
// // newUserDetails.getAuthorities());
// // SecurityContextHolder.getContext().setAuthentication(newAuth);
// // }

// UserDetails newUserDetails = new CustomUserDetails(updatedUser);
// Authentication newAuth = new UsernamePasswordAuthenticationToken(
// newUserDetails,
// null, // Credentials tidak perlu diisi ulang
// newUserDetails.getAuthorities() // Ambil roles/authorities yang sudah ada
// );

// // Set otentikasi baru ke dalam konteks keamanan
// SecurityContextHolder.getContext().setAuthentication(newAuth);
// // =============================================================

// return updatedUser;
// }

// @Override
// public User updateProfile(String username, ProfileDto profileDto) throws
// IOException {
// User user = userRepository.findByUsername(username)
// .orElseThrow(() -> new EntityNotFoundException("User not found: " +
// username));

// // Logika update foto & tanda tangan

// // // Proses update foto dan tanda tangan (jika ada)
// if (profileDto.getPhoto() != null && !profileDto.getPhoto().isEmpty()) {
// String fileName = fileStorageService.storeFile(profileDto.getPhoto());
// user.setPhotoPath(fileName);
// }

// // if (profileDto.getPhoto() != null && !profileDto.getPhoto().isEmpty()) {
// // String fileName = fileStorageService.storeFile(profileDto.getPhoto());
// // user.setPhotoPath(fileName);
// // }

// if (profileDto.getSignatureDataUrl() != null &&
// !profileDto.getSignatureDataUrl().isEmpty()) {
// String fileName =
// fileStorageService.storeBase64File(profileDto.getSignatureDataUrl());
// user.setSignaturePath(fileName);
// }

// // Update data dari form edit (termasuk username & email)
// user.setFullName(profileDto.getFullName());
// user.setPosition(profileDto.getPosition());
// user.setPhoneNumber(profileDto.getPhoneNumber());
// user.setAddress(profileDto.getAddress());
// user.setEmail(profileDto.getEmail());
// user.setUsername(profileDto.getUsername());

// User updatedUser = userRepository.save(user);

// // Segarkan sesi otentikasi
// refreshUserSession(updatedUser);

// return updatedUser;
// }