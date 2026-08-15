package com.example.EduGrid.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
public class Horario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private int turno;
    private int periodoDia;
    private int diaSemana;
   

    public Horario(Integer id, int turno, int periodoDia, int diaSemana) {
        this.id = id;
        this.turno = turno;
        this.periodoDia = periodoDia;
        this.diaSemana = diaSemana;
        
    }
    public Horario() {
       
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getTurno() {
        return turno;
    }

    public void setTurno(int turno) {
        this.turno = turno;
    }

    public int getPeriodoDia() {
        return periodoDia;
    }

    public void setPeriodoDia(int periodoDia) {
        this.periodoDia = periodoDia;
    }

    public int getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(int diaSemana) {
        this.diaSemana = diaSemana;
    }
}
