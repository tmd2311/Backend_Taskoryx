package com.taskoryx.backend.repository;

/**
 * Spring Data projection for GROUP BY file_type stats queries.
 * fileType → MIME string (e.g. "image/jpeg"), count → number of attachments.
 * FileCategory is derived in Java via FileCategory.fromMimeType(fileType).
 */
public interface AttachmentCategoryStatsProjection {
    String getFileType();
    Long getCount();
}
