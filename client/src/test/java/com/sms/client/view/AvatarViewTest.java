package com.sms.client.view;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AvatarViewTest {

    @Test
    void initialsUseFirstAndLastWord() {
        assertEquals("NK", AvatarView.initialsOf("Nguyễn Minh Khang"));
        assertEquals("TA", AvatarView.initialsOf("Trần Ngọc Anh"));
        assertEquals("A", AvatarView.initialsOf("an"));
        assertEquals("ĐB", AvatarView.initialsOf("  đặng   quốc  bảo "));
    }

    @Test
    void blankNameGivesQuestionMark() {
        assertEquals("?", AvatarView.initialsOf(null));
        assertEquals("?", AvatarView.initialsOf("   "));
    }
}
