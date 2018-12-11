package uk.stumme.models

import uk.stumme.models.domain.Transfer

data class GetTransfersResponse(val transfers: List<Transfer>)