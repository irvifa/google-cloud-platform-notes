package com.github.irvifa.beam.pipeline.exceptions

class DataTypeNotFoundException(targetDataType: String) :
    Exception("Data type not found for: $targetDataType")
