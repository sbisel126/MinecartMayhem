package io.github.sbisel126.minecartMayhem;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.util.StringUtil;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@NullMarked
public class CommandHandler implements BasicCommand {

    private String[] commands;

    public CommandHandler(){
        commands = new String[]{"", "", ""};

    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args){
        for(int i=0;i<commands.length;i++){
            if(){

            }
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args){
        if(args.length == 0){
            return List.of(commands);
        }else if(args.length == 1){
           return StringUtil.copyPartialMatches(args[0], List.of(commands), new ArrayList<>());
        }
        return BasicCommand.super.suggest(commandSourceStack, args);
    }
}
