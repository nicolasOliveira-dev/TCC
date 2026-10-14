import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.EduGrid.model.Horario;
import com.example.EduGrid.repository.HorarioRepository;
@Service
public class HorarioService {

    @Autowired
    private HorarioRepository horarioRepository;

    public HorarioService(HorarioRepository horarioRepository) {
        this.horarioRepository = horarioRepository;
    }

    public void salvarHorario(Horario horario) {
        horarioRepository.save(horario);
    }

    public Horario buscarHorarioPorId(Integer id) {
        return horarioRepository.findById(id).orElse(null);
    }

    public void deletarHorario(Integer id) {
        horarioRepository.deleteById(id);
    }

}
