package com.example.noteshare.controller;

import com.example.noteshare.common.PageResponse;
import com.example.noteshare.dto.request.CreateNoteRequest;
import com.example.noteshare.dto.response.NoteResponse;
import com.example.noteshare.security.UserDetailsImpl;
import com.example.noteshare.service.CommentService;
import com.example.noteshare.service.LikeService;
import com.example.noteshare.service.NoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NoteController.class)
@AutoConfigureMockMvc(addFilters = false)
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NoteService noteService;

    @MockBean
    private LikeService likeService;

    @MockBean
    private CommentService commentService;

    @MockBean
    private com.example.noteshare.security.JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        UserDetailsImpl userDetails = new UserDetailsImpl(1L);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void createNote_Success() throws Exception {
        CreateNoteRequest req = new CreateNoteRequest();
        req.setTitle("Test Note");
        req.setContent("This is a test note.");

        NoteResponse res = new NoteResponse();
        res.setId(100L);
        res.setTitle("Test Note");
        res.setContent("This is a test note.");

        when(noteService.createNote(eq(1L), any(CreateNoteRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.title").value("Test Note"));
    }

    @Test
    void listNotes_Success() throws Exception {
        PageResponse<NoteResponse> pageResponse = new PageResponse<>();
        pageResponse.setItems(Collections.emptyList());
        pageResponse.setTotal(0);

        when(noteService.listNotes(1, 20)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/notes")
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
