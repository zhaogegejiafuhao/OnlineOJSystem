package cn.edu.zjnu.acm.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 兼容线上旧库结构的轻量级迁移：
 * - solution.info 早期为 VARCHAR(255)，当判题返回长编译信息时会写库失败，导致一直 Pending。
 * - 同类风险：contest/team 描述、operation_log 错误信息、image_log URL/路径等字段也可能超过 255。
 * - 这里在启动时检测并自动升级为 TEXT/LONGTEXT（幂等）。
 */
@Slf4j
@Component
public class SchemaMigration {
    private final JdbcTemplate jdbcTemplate;

    public SchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        migrateSolutionInfoToLongText();
        migrateCommonVarchar255ToText();
        migrateOtherVarCharsToTextOrLongText();
    }

    private void migrateSolutionInfoToLongText() {
        try {
            String dataType = jdbcTemplate.queryForObject(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'solution' AND COLUMN_NAME = 'info'",
                    String.class
            );
            Long maxLen = jdbcTemplate.queryForObject(
                    "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'solution' AND COLUMN_NAME = 'info'",
                    Long.class
            );

            if (dataType == null) {
                log.warn("SchemaMigration: cannot detect solution.info column type (DATA_TYPE is null)");
                return;
            }

            if ("varchar".equalsIgnoreCase(dataType)) {
                log.warn("SchemaMigration: upgrading solution.info from VARCHAR({}) to LONGTEXT to prevent truncation",
                        maxLen);
                jdbcTemplate.execute("ALTER TABLE " + q("solution") + " MODIFY COLUMN " + q("info") + " LONGTEXT NOT NULL");
                log.info("SchemaMigration: solution.info upgraded to LONGTEXT");
            } else {
                log.debug("SchemaMigration: solution.info already {}, no migration needed", dataType);
            }
        } catch (Exception e) {
            // 不阻断启动：失败时仍有 Service 层截断兜底
            log.error("SchemaMigration: failed to migrate solution.info to LONGTEXT", e);
        }
    }

    private void migrateCommonVarchar255ToText() {
        // 这些列均不参与索引（已核对），改为 TEXT 更安全
        migrateVarchar255ToTextIfNeeded("contest", "description", true);
        migrateVarchar255ToTextIfNeeded("team", "description", true);
        migrateVarchar255ToTextIfNeeded("operation_log", "error_message", false);
        migrateVarchar255ToTextIfNeeded("operation_log", "resource", true);

        migrateVarchar255ToTextIfNeeded("image_log", "address", true);
        migrateVarchar255ToTextIfNeeded("image_log", "filename", true);
        migrateVarchar255ToTextIfNeeded("image_log", "url", true);

        migrateVarchar255ToTextIfNeeded("contest_problem", "temp_title", true);
        migrateVarchar255ToTextIfNeeded("data_export_task", "file_path", false);
        migrateVarchar255ToTextIfNeeded("backup_task", "storage_location", false);
    }

    private void migrateVarchar255ToTextIfNeeded(String table, String column, boolean notNull) {
        try {
            String dataType = jdbcTemplate.queryForObject(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    new Object[]{table, column},
                    String.class
            );
            Long maxLen = jdbcTemplate.queryForObject(
                    "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    new Object[]{table, column},
                    Long.class
            );
            if (dataType == null) {
                log.warn("SchemaMigration: cannot detect {}.{} column type (DATA_TYPE is null)", table, column);
                return;
            }
            if ("varchar".equalsIgnoreCase(dataType) && (maxLen == null || maxLen == 255L)) {
                log.warn("SchemaMigration: upgrading {}.{} from VARCHAR({}) to TEXT", table, column, maxLen);
                jdbcTemplate.execute("ALTER TABLE " + q(table) + " MODIFY COLUMN " + q(column) + " TEXT " + (notNull ? "NOT NULL" : "NULL"));
                log.info("SchemaMigration: {}.{} upgraded to TEXT", table, column);
            } else {
                log.debug("SchemaMigration: {}.{} already {}({}), no migration needed", table, column, dataType, maxLen);
            }
        } catch (Exception e) {
            log.error("SchemaMigration: failed to migrate {}.{} to TEXT", table, column, e);
        }
    }

    private void migrateOtherVarCharsToTextOrLongText() {
        // 这些列长度本身就超过 255，仍可能溢出/被截断；升级为 TEXT/LONGTEXT 更稳妥
        migrateVarcharExactLenToTextIfNeeded("operation_log", "details", false, 1000);
        migrateVarcharExactLenToTextIfNeeded("error_alarm", "alarm_message", true, 250);
        migrateVarcharExactLenToTextIfNeeded("error_record", "error_message", false, 250);
        migrateVarcharExactLenToTextIfNeeded("error_category", "description", false, 250);
        migrateVarcharExactLenToTextIfNeeded("performance_log", "user_agent", false, 100);

        // analysis/article 正文：历史上为 VARCHAR(5000)，升级为 LONGTEXT
        migrateVarcharExactLenToLongTextIfNeeded("analysis", "text", true, 5000);
        migrateVarcharExactLenToLongTextIfNeeded("article", "text", true, 5000);
    }

    private void migrateVarcharExactLenToTextIfNeeded(String table, String column, boolean notNull, int expectedLen) {
        try {
            String dataType = jdbcTemplate.queryForObject(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    new Object[]{table, column},
                    String.class
            );
            Long maxLen = jdbcTemplate.queryForObject(
                    "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    new Object[]{table, column},
                    Long.class
            );
            if (!"varchar".equalsIgnoreCase(String.valueOf(dataType)) || maxLen == null || maxLen != (long)expectedLen) {
                return;
            }
            log.warn("SchemaMigration: upgrading {}.{} from VARCHAR({}) to TEXT", table, column, maxLen);
            jdbcTemplate.execute("ALTER TABLE " + q(table) + " MODIFY COLUMN " + q(column) + " TEXT " + (notNull ? "NOT NULL" : "NULL"));
            log.info("SchemaMigration: {}.{} upgraded to TEXT", table, column);
        } catch (Exception e) {
            log.error("SchemaMigration: failed to migrate {}.{} to TEXT", table, column, e);
        }
    }

    private void migrateVarcharExactLenToLongTextIfNeeded(String table, String column, boolean notNull, int expectedLen) {
        try {
            String dataType = jdbcTemplate.queryForObject(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    new Object[]{table, column},
                    String.class
            );
            Long maxLen = jdbcTemplate.queryForObject(
                    "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    new Object[]{table, column},
                    Long.class
            );
            if (!"varchar".equalsIgnoreCase(String.valueOf(dataType)) || maxLen == null || maxLen != (long)expectedLen) {
                return;
            }
            log.warn("SchemaMigration: upgrading {}.{} from VARCHAR({}) to LONGTEXT", table, column, maxLen);
            jdbcTemplate.execute("ALTER TABLE " + q(table) + " MODIFY COLUMN " + q(column) + " LONGTEXT " + (notNull ? "NOT NULL" : "NULL"));
            log.info("SchemaMigration: {}.{} upgraded to LONGTEXT", table, column);
        } catch (Exception e) {
            log.error("SchemaMigration: failed to migrate {}.{} to LONGTEXT", table, column, e);
        }
    }

    private String q(String ident) {
        return "`" + ident.replace("`", "``") + "`";
    }
}

