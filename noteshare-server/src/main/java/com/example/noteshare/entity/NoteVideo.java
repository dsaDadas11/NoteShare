package com.example.noteshare.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "note_videos")
public class NoteVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Column(nullable = false, length = 500)
    private String url;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
