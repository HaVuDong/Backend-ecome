package havudong.baocao.repository;

import havudong.baocao.entity.Voucher;
import havudong.baocao.entity.enums.VoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    
    Optional<Voucher> findByCode(String code);
    
    boolean existsByCode(String code);
    
    List<Voucher> findByStatus(VoucherStatus status);
    
    @Query("SELECT v FROM Voucher v WHERE v.status = 'ACTIVE' " +
           "AND v.startDate <= :now AND v.endDate >= :now " +
           "AND v.usedQuantity < v.totalQuantity")
    List<Voucher> findActiveVouchers(LocalDateTime now);
}
