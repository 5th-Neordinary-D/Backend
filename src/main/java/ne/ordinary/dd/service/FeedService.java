package ne.ordinary.dd.service;

import lombok.RequiredArgsConstructor;
import ne.ordinary.dd.core.exception.Exception404;
import ne.ordinary.dd.core.exception.Exception500;
import ne.ordinary.dd.domain.Comment;
import ne.ordinary.dd.domain.Feed;
import ne.ordinary.dd.domain.FeedLike;
import ne.ordinary.dd.domain.User;
import ne.ordinary.dd.model.CommentResponse;
import ne.ordinary.dd.model.FeedRequest;
import ne.ordinary.dd.model.FeedResponse;
import ne.ordinary.dd.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
@RequiredArgsConstructor()
@Service()
public class FeedService {

    private final UserRepository userRepository;
    private final FeedsRepository feedsRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final CommentService commentService;

    public FeedResponse.FeedDTO getFeed(Long id) {
        Feed feedPS = feedsRepository.findById(id).orElseThrow(
                () -> new Exception404("해당 피드를 찾을 수 없습니다.")
        );

        User userPS = userRepository.findById(id).orElseThrow(
                () -> new Exception404("해당 유저를 찾을 수 없습니다.")
        );

        List<Comment> comments = commentRepository.findByFeedId(feedPS.getId());
        List<CommentResponse.CommentDTO> commentDTOs = new ArrayList<>();
        for (Comment c : comments) {
            CommentResponse.CommentDTO commentDTO = new CommentResponse.CommentDTO(
                    c,
                    commentLikeRepository.countByCommentIdAndType(c.getCommentId(), 1),
                    commentLikeRepository.countByCommentIdAndType(c.getCommentId(), 2),
                    commentLikeRepository.countByCommentIdAndUserId(c.getCommentId(), userPS.getId()) >= 1 ? true : false,
                    new CommentResponse.AuthorDTO(c.getWriter())
            );
            List<Comment> reComments = commentRepository.findByParentId(c.getCommentId());
            List<CommentResponse.ReCommentDTO> reCommentDTOs = new ArrayList<>();

            for (Comment re : reComments) {
                CommentResponse.ReCommentDTO reCommentDTO = new CommentResponse.ReCommentDTO(
                        re,
                        commentLikeRepository.countByCommentIdAndType(re.getCommentId(), 1),
                        commentLikeRepository.countByCommentIdAndType(re.getCommentId(), 2),
                        commentLikeRepository.countByCommentIdAndUserId(re.getCommentId(), userPS.getId()) >= 1 ? true : false,
                        new CommentResponse.AuthorDTO(re.getWriter())
                );

                reCommentDTOs.add(reCommentDTO);
            }
            commentDTO.setComments(reCommentDTOs);

            commentDTOs.add(commentDTO);
        }

        Optional<FeedLike> feedLikeOP = feedLikeRepository.findByUserIdAndFeedId(userPS.getId(), feedPS.getId());
        FeedResponse.FeedDTO feedDTO = new FeedResponse.FeedDTO(
                feedPS,
                new FeedResponse.AuthorDTO(userPS),
                feedLikeOP.isPresent() && (feedLikeOP.get().getSympathy1() >= 1
                        || feedLikeOP.get().getSympathy2() >= 1
                        || feedLikeOP.get().getSympathy3() >= 1
                        || feedLikeOP.get().getSympathy4() >= 1
                        || feedLikeOP.get().getSympathy5() >= 1)
                        ? true : false,
                countComments(commentDTOs),
                commentDTOs
        );
        if (feedLikeOP.isPresent()) {
            FeedLike feedLike = feedLikeOP.get();
            feedDTO.setSympathy1(feedLike.getSympathy1());
            feedDTO.setSympathy2(feedLike.getSympathy2());
            feedDTO.setSympathy3(feedLike.getSympathy3());
            feedDTO.setSympathy4(feedLike.getSympathy4());
            feedDTO.setSympathy5(feedLike.getSympathy5());
        } else {
            feedDTO.setSympathy1(0);
            feedDTO.setSympathy2(0);
            feedDTO.setSympathy3(0);
            feedDTO.setSympathy4(0);
            feedDTO.setSympathy5(0);
        }

        return feedDTO;
    }

    private int countComments(List<CommentResponse.CommentDTO> comments) {
        int count = 0;

        for (CommentResponse.CommentDTO commentDTO : comments) {
            count++;
            for (CommentResponse.ReCommentDTO re : commentDTO.getComments()) {
                count++;
            }
        }

        return count;
    }

    @Transactional
    public void addFeed(FeedRequest.AddDTO addDTO) {
        List<User> users = userRepository.findByUuid(addDTO.getUuid());
        if (users.size() == 0) {
            throw new Exception404("존재하지 않는 유저입니다.");
        }
        try {
            feedsRepository.save(addDTO.toEntity(users.get(0)));
        } catch (Exception e) {
            throw new Exception500("피드 저장에 실패했습니다.");
        }
    }

    @Transactional
    public void updateFeed(Long id, FeedRequest.UpdateDTO updateDTO) {
        Feed feedPS = feedsRepository.findById(id).orElseThrow(
                () -> new Exception404("존재하지 않는 피드입니다.")
        );
        try {
            if (updateDTO.getTitle() != null) {
                feedPS.updateTitle(updateDTO.getTitle());
            }
            if (updateDTO.getCategory() != null) {
                feedPS.updateCategory(updateDTO.getCategory());
            }
            if (updateDTO.getContent() != null) {
                feedPS.updateContent(updateDTO.getContent());
            }
        } catch (Exception e) {
            throw new Exception500("피드 수정이 실패했습니다.");
        }
    }

    @Transactional
    public void deleteFeed(Long id, FeedRequest.DeleteDTO deleteDTO) {
        try {
            userRepository.findByUuid(deleteDTO.getUuid());
        } catch (Exception e) {
            throw new Exception404("존재하지 않는 유저입니다.");
        }
        Feed feedPS = feedsRepository.findById(id).orElseThrow(
                () -> new Exception404("존재하지 않는 피드입니다.")
        );

        try {
            commentLikeRepository.deleteByFeedId(feedPS.getId());
            List<Comment> comments = commentRepository.findByFeedId(feedPS.getId());
            for (Comment c : comments) {
                commentService.deleteComment(c.getCommentId());
            }
            feedLikeRepository.deleteByFeedId(feedPS.getId());
            feedsRepository.deleteById(feedPS.getId());
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception500("피드 삭제가 실패했습니다.");
        }
    }

    @Transactional
    public void deleteFeedLike(Long id, FeedRequest.DeleteLikeDTO deleteLikeDTO) {
        Feed feedPS = feedsRepository.findById(id).orElseThrow(
                () -> new Exception404("존재하지 않는 피드입니다.")
        );
        List<User> users = userRepository.findByUuid(deleteLikeDTO.getUuid());
        if (users.size() == 0) {
            throw new Exception404("존재하지 않는 유저입니다.");
        }
        Optional<FeedLike> feedLikeOP = feedLikeRepository.findByFeedAndUser(feedPS, users.get(0));
        try {
            if (feedLikeOP.isPresent()) {
                feedLikeRepository.delete(feedLikeOP.get());
            }
        } catch (Exception e) {
            throw new Exception500("피드 공감 삭제가 실패했습니다.");
        }
    }
}
