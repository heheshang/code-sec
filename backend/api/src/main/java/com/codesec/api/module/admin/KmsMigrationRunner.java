package com.codesec.api.module.admin;

import com.codesec.api.domain.entity.RepoEntity;
import com.codesec.api.domain.repository.RepoRepository;
import com.codesec.common.crypto.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-time migration runner that re-encrypts AES-encrypted repo tokens using KMS.
 * <p>
 * Activated via {@code --spring.profiles.active=kms-migrate}.
 * Supports dry-run mode via {@code --kms-migration.dry-run=true}.
 * <p>
 * Usage:
 * <pre>
 *   java -jar api.jar --spring.profiles.active=kms-migrate --kms-migration.dry-run=true
 *   java -jar api.jar --spring.profiles.active=kms-migrate --kms-migration.dry-run=false
 * </pre>
 */
@Component
@Profile("kms-migrate")
public class KmsMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KmsMigrationRunner.class);

    private final RepoRepository repoRepo;
    private final CryptoService cryptoService;
    private final boolean dryRun;

    public KmsMigrationRunner(RepoRepository repoRepo,
                              CryptoService cryptoService,
                              @org.springframework.beans.factory.annotation.Value("${kms-migration.dry-run:true}") boolean dryRun) {
        this.repoRepo = repoRepo;
        this.cryptoService = cryptoService;
        this.dryRun = dryRun;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("KMS migration started (dry-run={})", dryRun);

        List<RepoEntity> repos = repoRepo.findAll();
        int total = repos.size();
        int migrated = 0;
        int skipped = 0;

        for (RepoEntity repo : repos) {
            String oldToken = repo.getAccessTokenEncrypted();
            String oldSecret = repo.getWebhookSecret();

            if ((oldToken == null || oldToken.isEmpty()) && (oldSecret == null || oldSecret.isEmpty())) {
                skipped++;
                continue;
            }

            log.info("  [{}] repo id={}: re-encrypting...", migrated + 1, repo.getId());

            if (!dryRun) {
                // Decrypt with old AES provider, re-encrypt with current (KMS) provider
                if (oldToken != null && !oldToken.isEmpty()) {
                    String decrypted = cryptoService.decrypt(oldToken);
                    repo.setAccessTokenEncrypted(cryptoService.encrypt(decrypted));
                }
                if (oldSecret != null && !oldSecret.isEmpty()) {
                    String decrypted = cryptoService.decrypt(oldSecret);
                    repo.setWebhookSecret(cryptoService.encrypt(decrypted));
                }
                repoRepo.save(repo);
            }

            migrated++;
        }

        log.info("KMS migration complete: total={}, migrated={}, skipped={}", total, migrated, skipped);

        if (dryRun && migrated > 0) {
            log.info("Run with --kms-migration.dry-run=false to execute the migration.");
        }

        // Shut down after migration completes (this is a one-shot runner)
        if (!dryRun) {
            log.info("Migration executed. Application will now exit.");
            System.exit(0);
        }
    }
}
