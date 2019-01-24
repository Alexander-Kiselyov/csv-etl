package com.example.batch.processing;

import org.apache.commons.validator.routines.CreditCardValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processor that passes through only lines containing a credit card number (16 consecutive digits +
 * Luhn algorithm match) and ignores all others.
 */
@Component
public class LineProcessorImpl implements LineProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LineProcessorImpl.class);

    /**
     * 16 consecutive digits that either:
     * <ol><li>start a line
     * <li>end a line
     * <li>are in the middle of a CSV line (i.e. has both trailing and leading commas)
     * </ol>
     * <b>Note: </b>any leading/trailing whitespaces will lead to a non-match since whitespaces are
     * significant according to RFC4180 and we must not tolerate them according to requirements.
     */
    /*
     * XXX: unfortunately, all regexps in org.apache.commons.validator.routines.CreditCardValidator don't tolerate card numbers in the middle of a line, so can't be reused.
     */
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("(?:^|,)(\\d{16})(?:$|,)");

    /**
     * Validator that assumes Visa/Mastercard only.
     */
    // XXX: could be made configurable, but who cares?
    private static final CreditCardValidator CREDIT_CARD_VALIDATOR = new CreditCardValidator(
            CreditCardValidator.VISA | CreditCardValidator.MASTERCARD);

    @Override
    public Object process(String line) {

        // TODO: add filename and line to MDC context for a comprehensive logging

        final Matcher cardNumMatcher = CREDIT_CARD_PATTERN.matcher(line);
        int startMatchIndex = 0;
        while (cardNumMatcher.find(startMatchIndex)) {
            LOGGER.trace("{} line matches", line);

            final String potentialCardNum = cardNumMatcher.group(1);

            LOGGER.trace("Potential card number is {}", potentialCardNum);

            if (CREDIT_CARD_VALIDATOR.isValid(potentialCardNum)) {

                LOGGER.trace("Potential card number {} considered valid.", potentialCardNum);

                return line;
            }

            LOGGER.trace("Potential card number {} is invalid.", potentialCardNum);

            startMatchIndex = cardNumMatcher.end() - 1;
        }

        return null;
    }

}
