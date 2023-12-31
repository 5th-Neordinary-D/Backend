package ne.ordinary.dd.service;

import lombok.RequiredArgsConstructor;
import ne.ordinary.dd.core.exception.Exception500;
import ne.ordinary.dd.domain.User;
import ne.ordinary.dd.model.UserRequestDto;
import ne.ordinary.dd.model.UserResponseDto;
import ne.ordinary.dd.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public Long register(User user){
        List<User> findList = userRepository.findByUuid(user.getUuid());

        if(findList.size() > 0) {
            throw new Exception500("user register error");
        }

        return userRepository.save(user);
    }

    public UserResponseDto login(String uuid){
        List<User> findList = userRepository.findByUuid(uuid);
        User findUser = null;
        System.out.println(findList.size());

        if(findList.size() == 1) {
            findUser = findList.get(0);
        }else{
            throw new Exception500("user login error");
        }


        return UserResponseDto.builder()
                .id(findUser.getId())
                .isCheck(findUser.isCheck())
                .build();
    }

    @Transactional
    public void rename(Long id, String rename){
        User user = userRepository.findUser(id);
        user.rename(rename);
    }

    @Transactional
    public void editHearLevel(Long id, int editHearLevel){
        User user = userRepository.findUser(id);
        user.editHearLevel(editHearLevel);
    }

}
