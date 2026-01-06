package havudong.baocao.repository;

import havudong.baocao.entity.UserPreference;
import havudong.baocao.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    
    Optional<UserPreference> findByUser(User user);
}
