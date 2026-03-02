package com.mealio.port.in;

import com.mealio.dto.HeadcountResponse;
import com.mealio.model.entity.Mess;

import java.util.UUID;

/**
 * Use-case interface: cook's dashboard headcount.
 */
public interface HeadcountUseCase {

    HeadcountResponse getHeadcount(UUID messId);

    /** Called after every meal toggle to keep the cache hot. */
    void refreshCache(Mess mess);
}
