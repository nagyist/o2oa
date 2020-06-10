//
//  IMChatMessageViewCell.swift
//  O2Platform
//
//  Created by FancyLou on 2020/6/8.
//  Copyright © 2020 zoneland. All rights reserved.
//

import UIKit
import CocoaLumberjack

class IMChatMessageViewCell: UITableViewCell {

    @IBOutlet weak var avatarImage: UIImageView!
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var timeLabel: UILabel!
    @IBOutlet weak var messageBackgroundView: UIView!
    @IBOutlet weak var messageBackgroundWidth: NSLayoutConstraint!
    @IBOutlet weak var messageBackgroundHeight: NSLayoutConstraint!
    private let messageWidth = 176
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
        // Configure the view for the selected state
    }
    
    func setContent(item: IMMessageInfo) {
        //time
        if let time = item.createTime {
            let date = time.toDate(formatter: "yyyy-MM-dd HH:mm:ss")
            self.timeLabel.text = date.friendlyTime()
        }
        //name avatart
        if let person = item.createPerson {
            let urlstr = AppDelegate.o2Collect.generateURLWithAppContextKey(ContactContext.contactsContextKeyV2, query: ContactContext.personIconByNameQueryV2, parameter: ["##name##":person as AnyObject], generateTime: false)
            if let u = URL(string: urlstr!) {
                self.avatarImage.hnk_setImageFromURL(u)
            }else {
                self.avatarImage.image = UIImage(named: "icon_men")
            }
            //姓名
            self.titleLabel.text = person.split("@").first ?? ""
        }else {
            self.avatarImage.image = UIImage(named: "icon_men")
            self.titleLabel.text = ""
        }
        self.messageBackgroundView.removeSubviews()
        if let jsonBody = item.body, let body = parseJson(msg: jsonBody) {
            if body.type == o2_im_msg_type_emoji {
                emojiMsgRender(emoji: body.body!)
            }else {
                textMsgRender(msg: body.body!)
            }
        }
    }
    
    private func emojiMsgRender(emoji: String) {
        let emojiSize = 36
        let width = CGFloat(emojiSize + 20)
        let height = CGFloat(emojiSize + 20)
        self.messageBackgroundWidth.constant = width
        self.messageBackgroundHeight.constant = height
        //背景图片
        let bgImg = UIImageView(frame: CGRect(x: 0, y: 0, width: width, height: height))
        let insets = UIEdgeInsets(top: 28, left: 10, bottom: 5, right: 5); // 上、左、下、右
        var bubble = UIImage(named: "chat_bubble_incomming")
        bubble = bubble?.resizableImage(withCapInsets: insets, resizingMode: .stretch)
        bgImg.image = bubble
        self.messageBackgroundView.addSubview(bgImg)
        //表情图
        let emojiImage = UIImageView(frame: CGRect(x: 0, y: 0, width: emojiSize, height: emojiSize))
        let bundle = Bundle().o2EmojiBundle(anyClass: IMChatMessageViewCell.self)
        let path = o2ImEmojiPath(emojiBody: emoji)
        emojiImage.image = UIImage(named: path, in: bundle, compatibleWith: nil)
        emojiImage.translatesAutoresizingMaskIntoConstraints = false
        self.messageBackgroundView.addSubview(emojiImage)
        let top = NSLayoutConstraint(item: emojiImage, attribute: .top, relatedBy: .equal, toItem: emojiImage.superview!, attribute: .top, multiplier: 1, constant: 10)
        let bottom = NSLayoutConstraint(item: emojiImage.superview! , attribute: .bottom, relatedBy: .equal, toItem: emojiImage, attribute: .bottom, multiplier: 1, constant: 10)
        let left = NSLayoutConstraint(item: emojiImage, attribute: .leading, relatedBy: .equal, toItem: emojiImage.superview!, attribute: .leading, multiplier: 1, constant: 10)
        let right = NSLayoutConstraint(item: emojiImage.superview!, attribute: .trailing, relatedBy: .equal, toItem: emojiImage, attribute: .trailing, multiplier: 1, constant: 10)
        NSLayoutConstraint.activate([top, bottom, left, right])
    }
    
    private func textMsgRender(msg: String) {
        let size = calTextSize(str: msg)
        self.messageBackgroundWidth.constant = size.width + 20
        self.messageBackgroundHeight.constant = size.height + 20
        //背景图片
        let bgImg = UIImageView(frame: CGRect(x: 0, y: 0, width: size.width + 20, height: size.height + 20))
        let insets = UIEdgeInsets(top: 28, left: 10, bottom: 5, right: 5); // 上、左、下、右
        var bubble = UIImage(named: "chat_bubble_incomming")
        bubble = bubble?.resizableImage(withCapInsets: insets, resizingMode: .stretch)
        bgImg.image = bubble
        self.messageBackgroundView.addSubview(bgImg)
        //文字
        let label = generateMessagelabel(str: msg, size: size)
        label.translatesAutoresizingMaskIntoConstraints = false
        self.messageBackgroundView.addSubview(label)
        let top = NSLayoutConstraint(item: label, attribute: .top, relatedBy: .equal, toItem: label.superview!, attribute: .top, multiplier: 1, constant: 10)
//        let bottom = NSLayoutConstraint(item: label.superview! , attribute: .bottom, relatedBy: .equal, toItem: label, attribute: .bottom, multiplier: 1, constant: 10)
        let left = NSLayoutConstraint(item: label, attribute: .leading, relatedBy: .equal, toItem: label.superview!, attribute: .leading, multiplier: 1, constant: 10)
        let right = NSLayoutConstraint(item: label.superview!, attribute: .trailing, relatedBy: .equal, toItem: label, attribute: .trailing, multiplier: 1, constant: 10)
        NSLayoutConstraint.activate([top, left, right])
    }
    
    private func generateMessagelabel(str: String, size: CGSize) -> UILabel {
        let label = UILabel(frame: CGRect(x: 0, y: 0, width: size.width, height: size.height))
        label.text = str
        label.font = UIFont.systemFont(ofSize: 16)
        label.numberOfLines = 0
        label.lineBreakMode = .byCharWrapping
        label.preferredMaxLayoutWidth = size.width
        return label
    }
    
    
    private func calTextSize(str: String) -> CGSize {
        let size = CGSize(width: messageWidth.toCGFloat, height: CGFloat(MAXFLOAT))
        return str.boundingRect(with: size, options: .usesLineFragmentOrigin, attributes: [NSAttributedString.Key.font: UIFont.systemFont(ofSize: 16)], context: nil).size
    }
    
    //解析json为消息对象
    private func parseJson(msg: String) -> IMMessageBodyInfo? {
        return IMMessageBodyInfo.deserialize(from: msg)
    }
}
