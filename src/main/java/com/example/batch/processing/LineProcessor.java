package com.example.batch.processing;

import org.springframework.batch.item.ItemProcessor;

public interface LineProcessor extends ItemProcessor<String, Object> {

}
