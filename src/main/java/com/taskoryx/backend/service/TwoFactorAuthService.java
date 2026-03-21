package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.auth.TwoFactorDisableRequest;
import com.taskoryx.backend.dto.request.auth.TwoFactorVerifyRequest;
import com.taskoryx.backend.dto.response.auth.TwoFactorSetupResponse;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TwoFactorSetupResponse setup2FA(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        if (user.isTwoFactorEnabled()) {
            throw new BadRequestException("Xác thực 2 bước đã được kích hoạt");
        }

        // Generate secret
        String secret = new DefaultSecretGenerator().generate();

        // Store pending secret (not enabled yet - user needs to verify first)
        user.setTwoFactorSecret(secret);
        userRepository.save(user);

        // Generate QR code URI
        QrData qrData = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer("Taskoryx")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        String qrCodeUrl = "";
        try {
            QrGenerator generator = new ZxingPngQrGenerator();
            byte[] imageData = generator.generate(qrData);
            qrCodeUrl = getDataUriForImage(imageData, generator.getImageMimeType());
        } catch (QrGenerationException e) {
            log.error("Failed to generate QR code", e);
            qrCodeUrl = qrData.getUri(); // fallback to URI
        }

        return TwoFactorSetupResponse.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .manualCode(secret)
                .build();
    }

    @Transactional
    public void enable2FA(TwoFactorVerifyRequest request, UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        if (user.getTwoFactorSecret() == null) {
            throw new BadRequestException("Hãy thiết lập 2FA trước khi kích hoạt");
        }

        if (!verifyCode(user.getTwoFactorSecret(), request.getCode())) {
            throw new BadRequestException("Mã xác thực không hợp lệ");
        }

        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void disable2FA(TwoFactorDisableRequest request, UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        if (!user.isTwoFactorEnabled()) {
            throw new BadRequestException("Xác thực 2 bước chưa được kích hoạt");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu không đúng");
        }

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
    }

    public boolean verifyCode(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return verifier.isValidCode(secret, code);
    }

    public boolean is2FAEnabled(UUID userId) {
        return userRepository.findById(userId)
                .map(User::isTwoFactorEnabled)
                .orElse(false);
    }
}
