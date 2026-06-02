import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  imports: [RouterLink],
  templateUrl: './landing.component.html',
})
export class LandingComponent {
  readonly features = [
    {
      title: 'Rank-Based Analysis',
      description: 'Compare your EAMCET rank against historical closing ranks for your category.',
      icon: '📊',
    },
    {
      title: 'Dream Colleges',
      description: 'Reach options — more competitive colleges with lower historical closing ranks.',
      icon: '🎯',
    },
    {
      title: 'Target Colleges',
      description: 'Balanced matches where your rank aligns closely with past closing ranks.',
      icon: '⚖️',
    },
    {
      title: 'Safe Colleges',
      description: 'Solid backup choices with higher closing ranks and a comfortable margin.',
      icon: '🛡️',
    },
    {
      title: 'Latest Cutoff Data',
      description: 'Recommendations powered by official TG EAPCET cutoff records.',
      icon: '📅',
    },
    {
      title: 'Branch Recommendations',
      description: 'Filter by your preferred branches — CSE, ECE, IT, and more.',
      icon: '🏫',
    },
  ];

  readonly steps = [
    'Enter your rank',
    'Select category & gender',
    'Choose preferred branches',
    'Get Dream, Target & Safe lists',
  ];
}
